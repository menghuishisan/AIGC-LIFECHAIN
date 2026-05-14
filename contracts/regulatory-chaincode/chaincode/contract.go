package chaincode

import (
	"encoding/json"
	"fmt"
	"time"

	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

const (
	ChaincodeVersion     = "v1"
	DocTypeFreeze        = "freeze"
	DocTypeDispute       = "dispute"
	DocTypeReport        = "report"
	StatusFrozen         = "FREEZE_EFFECTIVE"
	StatusUnfrozen       = "UNFREEZE_EFFECTIVE"
	StatusDisputeSuccess = "DISPUTE_RESOLVED"
	StatusReportSuccess  = "REPORT_REGISTERED"
)

// RegulatoryContract 实现监管链码的所有合约函数
type RegulatoryContract struct {
	contractapi.Contract
}

func freezeKey(freezeNo string) string {
	return fmt.Sprintf("FREEZE#%s", freezeNo)
}

func disputeKey(caseNo string) string {
	return fmt.Sprintf("DISPUTE#%s", caseNo)
}

func reportKey(reportNo string) string {
	return fmt.Sprintf("REPORT#%s", reportNo)
}

func txTime(ctx contractapi.TransactionContextInterface) string {
	t, err := ctx.GetStub().GetTxTimestamp()
	if err != nil {
		return time.Now().UTC().Format(time.RFC3339)
	}
	return time.Unix(t.Seconds, int64(t.Nanos)).UTC().Format(time.RFC3339)
}

func emitEvent(ctx contractapi.TransactionContextInterface, eventName, bizType, bizNo, status string) {
	payload := EventPayload{
		BizType: bizType,
		BizNo:   bizNo,
		TxTime:  txTime(ctx),
		Status:  status,
	}
	b, _ := json.Marshal(payload)
	_ = ctx.GetStub().SetEvent(eventName, b)
}

// RegisterFreeze 注册冻结记录到账本
// 幂等：同一 freezeNo 已存在且载荷相同则幂等成功；载荷不同则返回冲突错误
func (c *RegulatoryContract) RegisterFreeze(ctx contractapi.TransactionContextInterface, payloadJSON string) error {
	var req RegisterFreezeRequest
	if err := json.Unmarshal([]byte(payloadJSON), &req); err != nil {
		return fmt.Errorf("invalid payload: %w", err)
	}
	if req.FreezeNo == "" || req.TargetNo == "" || req.TargetType == "" || req.Reason == "" {
		return fmt.Errorf("freezeNo, targetType, targetNo, reason are required")
	}

	key := freezeKey(req.FreezeNo)
	existing, err := ctx.GetStub().GetState(key)
	if err != nil {
		return fmt.Errorf("failed to read ledger: %w", err)
	}
	if existing != nil {
		var existingLedger FreezeLedger
		if err = json.Unmarshal(existing, &existingLedger); err != nil {
			return fmt.Errorf("failed to unmarshal existing ledger: %w", err)
		}
		if existingLedger.TargetType == req.TargetType && existingLedger.TargetNo == req.TargetNo {
			return nil
		}
		return fmt.Errorf("CONFLICT: freeze record already exists with different payload: %s", req.FreezeNo)
	}

	now := txTime(ctx)
	freezeTime := req.FreezeTime
	if freezeTime == "" {
		freezeTime = now
	}

	ledger := FreezeLedger{
		DocType:          DocTypeFreeze,
		FreezeNo:         req.FreezeNo,
		TargetType:       req.TargetType,
		TargetNo:         req.TargetNo,
		Reason:           req.Reason,
		Status:           StatusFrozen,
		FreezeTime:       freezeTime,
		TxTime:           now,
		ChaincodeVersion: ChaincodeVersion,
	}

	b, err := json.Marshal(ledger)
	if err != nil {
		return fmt.Errorf("failed to marshal ledger: %w", err)
	}
	if err = ctx.GetStub().PutState(key, b); err != nil {
		return fmt.Errorf("failed to put state: %w", err)
	}

	emitEvent(ctx, "FreezeRegistered", "FREEZE", req.FreezeNo, StatusFrozen)
	return nil
}

// RegisterUnfreeze 在同一freezeNo的账本记录上追加解冻信息
func (c *RegulatoryContract) RegisterUnfreeze(ctx contractapi.TransactionContextInterface, payloadJSON string) error {
	var req RegisterUnfreezeRequest
	if err := json.Unmarshal([]byte(payloadJSON), &req); err != nil {
		return fmt.Errorf("invalid payload: %w", err)
	}
	if req.FreezeNo == "" {
		return fmt.Errorf("freezeNo is required")
	}

	key := freezeKey(req.FreezeNo)
	b, err := ctx.GetStub().GetState(key)
	if err != nil {
		return fmt.Errorf("failed to read ledger: %w", err)
	}
	if b == nil {
		return fmt.Errorf("freeze record not found: %s", req.FreezeNo)
	}

	var ledger FreezeLedger
	if err = json.Unmarshal(b, &ledger); err != nil {
		return fmt.Errorf("failed to unmarshal ledger: %w", err)
	}
	if ledger.Status == StatusUnfrozen {
		return fmt.Errorf("freeze record already unfrozen: %s", req.FreezeNo)
	}

	now := txTime(ctx)
	unfreezeTime := req.UnfreezeTime
	if unfreezeTime == "" {
		unfreezeTime = now
	}

	ledger.Status = StatusUnfrozen
	ledger.UnfreezeTime = unfreezeTime
	ledger.UnfreezeReason = req.Reason
	ledger.TxTime = now

	updated, err := json.Marshal(ledger)
	if err != nil {
		return fmt.Errorf("failed to marshal ledger: %w", err)
	}
	if err = ctx.GetStub().PutState(key, updated); err != nil {
		return fmt.Errorf("failed to put state: %w", err)
	}

	emitEvent(ctx, "UnfreezeRegistered", "FREEZE", req.FreezeNo, StatusUnfrozen)
	return nil
}

// RegisterDisputeConclusion 注册争议结论到账本
// 幂等：同一 caseNo 已存在则返回错误
func (c *RegulatoryContract) RegisterDisputeConclusion(ctx contractapi.TransactionContextInterface, payloadJSON string) error {
	var req RegisterDisputeConclusionRequest
	if err := json.Unmarshal([]byte(payloadJSON), &req); err != nil {
		return fmt.Errorf("invalid payload: %w", err)
	}
	if req.CaseNo == "" || req.Conclusion == "" || req.ResultSummary == "" {
		return fmt.Errorf("caseNo, conclusion, resultSummary are required")
	}

	key := disputeKey(req.CaseNo)
	existing, err := ctx.GetStub().GetState(key)
	if err != nil {
		return fmt.Errorf("failed to read ledger: %w", err)
	}
	if existing != nil {
		var existingLedger DisputeLedger
		if err = json.Unmarshal(existing, &existingLedger); err != nil {
			return fmt.Errorf("failed to unmarshal existing ledger: %w", err)
		}
		if existingLedger.Conclusion == req.Conclusion && existingLedger.ResultSummary == req.ResultSummary {
			return nil
		}
		return fmt.Errorf("CONFLICT: dispute conclusion already exists with different payload: %s", req.CaseNo)
	}

	now := txTime(ctx)
	closeTime := req.CloseTime
	if closeTime == "" {
		closeTime = now
	}

	ledger := DisputeLedger{
		DocType:          DocTypeDispute,
		CaseNo:           req.CaseNo,
		Conclusion:       req.Conclusion,
		ResultSummary:    req.ResultSummary,
		Status:           StatusDisputeSuccess,
		CloseTime:        closeTime,
		TxTime:           now,
		ChaincodeVersion: ChaincodeVersion,
	}

	b, err := json.Marshal(ledger)
	if err != nil {
		return fmt.Errorf("failed to marshal ledger: %w", err)
	}
	if err = ctx.GetStub().PutState(key, b); err != nil {
		return fmt.Errorf("failed to put state: %w", err)
	}

	emitEvent(ctx, "DisputeConclusionRegistered", "DISPUTE", req.CaseNo, StatusDisputeSuccess)
	return nil
}

// RegisterReport 注册监管报告摘要到账本
// 幂等：同一 reportNo 已存在且载荷相同则幂等成功；载荷不同则返回冲突错误
func (c *RegulatoryContract) RegisterReport(ctx contractapi.TransactionContextInterface, payloadJSON string) error {
	var req RegisterReportRequest
	if err := json.Unmarshal([]byte(payloadJSON), &req); err != nil {
		return fmt.Errorf("invalid payload: %w", err)
	}
	if req.ReportNo == "" || req.SummaryHash == "" {
		return fmt.Errorf("reportNo, summaryHash are required")
	}

	key := reportKey(req.ReportNo)
	existing, err := ctx.GetStub().GetState(key)
	if err != nil {
		return fmt.Errorf("failed to read ledger: %w", err)
	}
	if existing != nil {
		var existingLedger ReportLedger
		if err = json.Unmarshal(existing, &existingLedger); err != nil {
			return fmt.Errorf("failed to unmarshal existing ledger: %w", err)
		}
		if existingLedger.SummaryHash == req.SummaryHash {
			return nil
		}
		return fmt.Errorf("CONFLICT: report already exists with different payload: %s", req.ReportNo)
	}

	now := txTime(ctx)
	generateTime := req.GenerateTime
	if generateTime == "" {
		generateTime = now
	}

	ledger := ReportLedger{
		DocType:          DocTypeReport,
		ReportNo:         req.ReportNo,
		SummaryHash:      req.SummaryHash,
		Status:           StatusReportSuccess,
		GenerateTime:     generateTime,
		TxTime:           now,
		ChaincodeVersion: ChaincodeVersion,
	}

	b, err := json.Marshal(ledger)
	if err != nil {
		return fmt.Errorf("failed to marshal ledger: %w", err)
	}
	if err = ctx.GetStub().PutState(key, b); err != nil {
		return fmt.Errorf("failed to put state: %w", err)
	}

	emitEvent(ctx, "ReportRegistered", "REPORT", req.ReportNo, StatusReportSuccess)
	return nil
}

// QueryFreeze 查询冻结记录
func (c *RegulatoryContract) QueryFreeze(ctx contractapi.TransactionContextInterface, freezeNo string) (*FreezeLedger, error) {
	key := freezeKey(freezeNo)
	b, err := ctx.GetStub().GetState(key)
	if err != nil {
		return nil, fmt.Errorf("failed to read ledger: %w", err)
	}
	if b == nil {
		return nil, fmt.Errorf("freeze record not found: %s", freezeNo)
	}
	var ledger FreezeLedger
	if err = json.Unmarshal(b, &ledger); err != nil {
		return nil, fmt.Errorf("failed to unmarshal ledger: %w", err)
	}
	return &ledger, nil
}

// QueryDispute 查询争议结论
func (c *RegulatoryContract) QueryDispute(ctx contractapi.TransactionContextInterface, caseNo string) (*DisputeLedger, error) {
	key := disputeKey(caseNo)
	b, err := ctx.GetStub().GetState(key)
	if err != nil {
		return nil, fmt.Errorf("failed to read ledger: %w", err)
	}
	if b == nil {
		return nil, fmt.Errorf("dispute not found: %s", caseNo)
	}
	var ledger DisputeLedger
	if err = json.Unmarshal(b, &ledger); err != nil {
		return nil, fmt.Errorf("failed to unmarshal ledger: %w", err)
	}
	return &ledger, nil
}

// QueryReport 查询监管报告
func (c *RegulatoryContract) QueryReport(ctx contractapi.TransactionContextInterface, reportNo string) (*ReportLedger, error) {
	key := reportKey(reportNo)
	b, err := ctx.GetStub().GetState(key)
	if err != nil {
		return nil, fmt.Errorf("failed to read ledger: %w", err)
	}
	if b == nil {
		return nil, fmt.Errorf("report not found: %s", reportNo)
	}
	var ledger ReportLedger
	if err = json.Unmarshal(b, &ledger); err != nil {
		return nil, fmt.Errorf("failed to unmarshal ledger: %w", err)
	}
	return &ledger, nil
}

// GetHistoryByKey 查询指定key的完整历史记录
func (c *RegulatoryContract) GetHistoryByKey(ctx contractapi.TransactionContextInterface, key string) (string, error) {
	iter, err := ctx.GetStub().GetHistoryForKey(key)
	if err != nil {
		return "", fmt.Errorf("failed to get history: %w", err)
	}
	defer iter.Close()

	type HistoryRecord struct {
		TxID      string      `json:"txId"`
		Timestamp string      `json:"timestamp"`
		IsDelete  bool        `json:"isDelete"`
		Value     interface{} `json:"value"`
	}

	var records []HistoryRecord
	for iter.HasNext() {
		mod, err := iter.Next()
		if err != nil {
			return "", fmt.Errorf("iterator error: %w", err)
		}
		rec := HistoryRecord{
			TxID:      mod.TxId,
			Timestamp: time.Unix(mod.Timestamp.Seconds, int64(mod.Timestamp.Nanos)).UTC().Format(time.RFC3339),
			IsDelete:  mod.IsDelete,
		}
		if !mod.IsDelete && len(mod.Value) > 0 {
			var v interface{}
			if err = json.Unmarshal(mod.Value, &v); err == nil {
				rec.Value = v
			}
		}
		records = append(records, rec)
	}

	result, err := json.Marshal(records)
	if err != nil {
		return "", fmt.Errorf("failed to marshal history: %w", err)
	}
	return string(result), nil
}
