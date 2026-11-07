package chaincode

import (
	"encoding/json"
	"fmt"
	"time"

	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

const (
	ChaincodeVersion  = "v1"
	DocTypeSettlement = "settlement"
	DocTypeReversal   = "reverse_settlement"
	StatusSettled     = "SETTLE_SUCCESS"
	StatusReversed    = "REVERSE_SUCCESS"
)

// SettlementContract 实现结算链码的所有合约函数
type SettlementContract struct {
	contractapi.Contract
}

func settleKey(settleNo string) string {
	return fmt.Sprintf("SETTLE#%s", settleNo)
}

func reverseKey(reverseSettleNo string) string {
	return fmt.Sprintf("REVERSE_SETTLE#%s", reverseSettleNo)
}

// txTime 仅使用交易提案时间；无法读取时拒绝交易，避免各背书节点使用本地时钟。
func txTime(ctx contractapi.TransactionContextInterface) (string, error) {
	t, err := ctx.GetStub().GetTxTimestamp()
	if err != nil {
		return "", fmt.Errorf("failed to read transaction timestamp: %w", err)
	}
	if t == nil || t.Nanos < 0 || t.Nanos >= 1000000000 {
		return "", fmt.Errorf("invalid transaction timestamp")
	}
	return time.Unix(t.Seconds, int64(t.Nanos)).UTC().Format(time.RFC3339), nil
}

func emitEvent(ctx contractapi.TransactionContextInterface, eventName, bizType, bizNo, status string) error {
	now, err := txTime(ctx)
	if err != nil {
		return err
	}
	payload := EventPayload{BizType: bizType, BizNo: bizNo, TxTime: now, Status: status}
	b, err := json.Marshal(payload)
	if err != nil {
		return fmt.Errorf("failed to marshal event: %w", err)
	}
	return ctx.GetStub().SetEvent(eventName, b)
}

// RegisterSettlement 注册结算摘要到账本
// 幂等：同一 settleNo 已存在且载荷相同则幂等成功；载荷不同则返回冲突错误
func (c *SettlementContract) RegisterSettlement(ctx contractapi.TransactionContextInterface, payloadJSON string) error {
	var req RegisterSettlementRequest
	if err := json.Unmarshal([]byte(payloadJSON), &req); err != nil {
		return fmt.Errorf("invalid payload: %w", err)
	}
	if req.SettleNo == "" || req.OrderNo == "" || req.SummaryHash == "" {
		return fmt.Errorf("settleNo, orderNo, summaryHash are required")
	}
	if req.TotalAmount < 1 || req.TotalAmount > 10000000000 {
		return fmt.Errorf("totalAmount must be CNY integer fen within 1..10000000000")
	}
	if req.Currency != "CNY" || req.AmountUnit != "FEN" {
		return fmt.Errorf("settlement requires CNY integer FEN")
	}

	key := settleKey(req.SettleNo)
	existing, err := ctx.GetStub().GetState(key)
	if err != nil {
		return fmt.Errorf("failed to read ledger: %w", err)
	}
	if existing != nil {
		var existingLedger SettlementLedger
		if err = json.Unmarshal(existing, &existingLedger); err != nil {
			return fmt.Errorf("failed to unmarshal existing ledger: %w", err)
		}
		if existingLedger.OrderNo == req.OrderNo && existingLedger.SummaryHash == req.SummaryHash &&
			existingLedger.TotalAmount == req.TotalAmount && existingLedger.SettleTime == req.SettleTime {
			return nil
		}
		return fmt.Errorf("CONFLICT: settlement already exists with different payload: %s", req.SettleNo)
	}

	now, err := txTime(ctx)
	if err != nil {
		return err
	}
	settleTime := req.SettleTime
	if settleTime == "" {
		settleTime = now
	}

	ledger := SettlementLedger{
		DocType:          DocTypeSettlement,
		SettleNo:         req.SettleNo,
		OrderNo:          req.OrderNo,
		TotalAmount:      req.TotalAmount,
		Currency:         req.Currency,
		AmountUnit:       req.AmountUnit,
		SummaryHash:      req.SummaryHash,
		Status:           StatusSettled,
		SettleTime:       settleTime,
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

	return emitEvent(ctx, "SettlementRegistered", "SETTLEMENT", req.SettleNo, StatusSettled)
}

// RegisterReverseSettlement 注册逆分账摘要到账本
// 幂等：同一 reverseNo 已存在且载荷相同则幂等成功；载荷不同则返回冲突错误
func (c *SettlementContract) RegisterReverseSettlement(ctx contractapi.TransactionContextInterface, payloadJSON string) error {
	var req RegisterReverseSettlementRequest
	if err := json.Unmarshal([]byte(payloadJSON), &req); err != nil {
		return fmt.Errorf("invalid payload: %w", err)
	}
	if req.ReverseNo == "" || req.SettleNo == "" {
		return fmt.Errorf("reverseNo, settleNo are required")
	}
	if req.ReverseAmount < 1 || req.ReverseAmount > 10000000000 {
		return fmt.Errorf("reverseAmount must be CNY integer fen within 1..10000000000")
	}
	if req.Currency != "CNY" || req.AmountUnit != "FEN" {
		return fmt.Errorf("reversal requires CNY integer FEN")
	}

	// 校验原 settleNo 必须已存在于账本
	settleKeyStr := settleKey(req.SettleNo)
	settleBytes, err := ctx.GetStub().GetState(settleKeyStr)
	if err != nil {
		return fmt.Errorf("failed to read settlement ledger: %w", err)
	}
	if settleBytes == nil {
		return fmt.Errorf("settlement not found on ledger: %s", req.SettleNo)
	}
	var settlement SettlementLedger
	if err = json.Unmarshal(settleBytes, &settlement); err != nil {
		return fmt.Errorf("invalid settlement ledger: %w", err)
	}
	if req.ReverseAmount != settlement.TotalAmount {
		return fmt.Errorf("full refund reversal amount must equal original totalAmount")
	}

	key := reverseKey(req.ReverseNo)
	existing, err2 := ctx.GetStub().GetState(key)
	if err2 != nil {
		return fmt.Errorf("failed to read ledger: %w", err2)
	}
	if existing != nil {
		var existingLedger ReverseSettlementLedger
		if err2 = json.Unmarshal(existing, &existingLedger); err2 != nil {
			return fmt.Errorf("failed to unmarshal existing ledger: %w", err2)
		}
		if existingLedger.SettleNo == req.SettleNo && existingLedger.ReverseAmount == req.ReverseAmount &&
			existingLedger.Reason == req.Reason && existingLedger.ReverseTime == req.ReverseTime {
			return nil
		}
		return fmt.Errorf("CONFLICT: reverse settlement already exists with different payload: %s", req.ReverseNo)
	}
	// 同一原结算仅允许一笔全额逆转；读取和写入同一索引使Fabric MVCC阻止并发重复逆转。
	reversalIndex := "REVERSED#" + req.SettleNo
	prior, err := ctx.GetStub().GetState(reversalIndex)
	if err != nil {
		return fmt.Errorf("failed to read reversal index: %w", err)
	}
	if prior != nil {
		return fmt.Errorf("CONFLICT: settlement already reversed: %s", req.SettleNo)
	}

	now, err := txTime(ctx)
	if err != nil {
		return err
	}
	reverseTime := req.ReverseTime
	if reverseTime == "" {
		reverseTime = now
	}

	ledger := ReverseSettlementLedger{
		DocType:          DocTypeReversal,
		ReverseNo:        req.ReverseNo,
		SettleNo:         req.SettleNo,
		ReverseAmount:    req.ReverseAmount,
		Currency:         req.Currency,
		AmountUnit:       req.AmountUnit,
		Reason:           req.Reason,
		Status:           StatusReversed,
		ReverseTime:      reverseTime,
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
	if err = ctx.GetStub().PutState(reversalIndex, []byte(req.ReverseNo)); err != nil {
		return fmt.Errorf("failed to put reversal index: %w", err)
	}
	settlement.Status = StatusReversed
	settleUpdated, err := json.Marshal(settlement)
	if err != nil {
		return err
	}
	if err = ctx.GetStub().PutState(settleKeyStr, settleUpdated); err != nil {
		return err
	}
	return emitEvent(ctx, "ReverseSettlementRegistered", "REVERSE_SETTLEMENT", req.ReverseNo, StatusReversed)
}

// QuerySettlement 查询结算账本状态
func (c *SettlementContract) QuerySettlement(ctx contractapi.TransactionContextInterface, settleNo string) (*SettlementLedger, error) {
	key := settleKey(settleNo)
	b, err := ctx.GetStub().GetState(key)
	if err != nil {
		return nil, fmt.Errorf("failed to read ledger: %w", err)
	}
	if b == nil {
		return nil, fmt.Errorf("settlement not found: %s", settleNo)
	}
	var ledger SettlementLedger
	if err = json.Unmarshal(b, &ledger); err != nil {
		return nil, fmt.Errorf("failed to unmarshal ledger: %w", err)
	}
	return &ledger, nil
}

// QueryReverseSettlement 查询逆分账账本状态
func (c *SettlementContract) QueryReverseSettlement(ctx contractapi.TransactionContextInterface, reverseNo string) (*ReverseSettlementLedger, error) {
	key := reverseKey(reverseNo)
	b, err := ctx.GetStub().GetState(key)
	if err != nil {
		return nil, fmt.Errorf("failed to read ledger: %w", err)
	}
	if b == nil {
		return nil, fmt.Errorf("reverse settlement not found: %s", reverseNo)
	}
	var ledger ReverseSettlementLedger
	if err = json.Unmarshal(b, &ledger); err != nil {
		return nil, fmt.Errorf("failed to unmarshal ledger: %w", err)
	}
	return &ledger, nil
}

// GetHistoryByKey 查询指定key的完整历史记录（支持 SETTLE# 或 REVERSE_SETTLE# 前缀的完整key）
func (c *SettlementContract) GetHistoryByKey(ctx contractapi.TransactionContextInterface, ledgerKey string) (string, error) {
	iter, err := ctx.GetStub().GetHistoryForKey(ledgerKey)
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
		if !mod.IsDelete {
			var v interface{}
			_ = json.Unmarshal(mod.Value, &v)
			rec.Value = v
		}
		records = append(records, rec)
	}

	result, err := json.Marshal(records)
	if err != nil {
		return "", fmt.Errorf("failed to marshal history: %w", err)
	}
	return string(result), nil
}
