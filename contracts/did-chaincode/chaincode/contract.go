package chaincode

import (
	"encoding/json"
	"fmt"
	"time"

	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

const (
	ChaincodeVersion = "v1"
	DocTypeDid       = "did"
	StatusActive     = "DID_ACTIVE"
	StatusSuspended  = "DID_SUSPENDED"
	StatusRevoked    = "DID_REVOKED"
)

// DidContract 实现 DID 链码的所有合约函数
type DidContract struct {
	contractapi.Contract
}

// ledgerKey 生成账本Key
func ledgerKey(didNo string) string {
	return fmt.Sprintf("DID#%s", didNo)
}

// txTime 获取当前交易时间（UTC ISO8601）
func txTime(ctx contractapi.TransactionContextInterface) string {
	t, err := ctx.GetStub().GetTxTimestamp()
	if err != nil {
		return time.Now().UTC().Format(time.RFC3339)
	}
	return time.Unix(t.Seconds, int64(t.Nanos)).UTC().Format(time.RFC3339)
}

// emitEvent 发送链码事件
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

// RegisterDID 注册DID到账本
// 幂等：同一 didNo 已存在且载荷相同则幂等成功；载荷不同则返回冲突错误
func (c *DidContract) RegisterDID(ctx contractapi.TransactionContextInterface, payloadJSON string) error {
	var req RegisterDIDRequest
	if err := json.Unmarshal([]byte(payloadJSON), &req); err != nil {
		return fmt.Errorf("invalid payload: %w", err)
	}
	if req.DidNo == "" || req.DidValue == "" || req.AccountNo == "" {
		return fmt.Errorf("didNo, didValue, accountNo are required")
	}

	key := ledgerKey(req.DidNo)
	existing, err := ctx.GetStub().GetState(key)
	if err != nil {
		return fmt.Errorf("failed to read ledger: %w", err)
	}
	if existing != nil {
		var existingLedger DidLedger
		if err = json.Unmarshal(existing, &existingLedger); err != nil {
			return fmt.Errorf("failed to unmarshal existing ledger: %w", err)
		}
		// 幂等：载荷相同视为成功，载荷不同视为冲突
		if existingLedger.DidValue == req.DidValue && existingLedger.AccountNo == req.AccountNo {
			return nil
		}
		return fmt.Errorf("CONFLICT: DID already exists with different payload: %s", req.DidNo)
	}

	now := txTime(ctx)
	activeTime := req.ActiveTime
	if activeTime == "" {
		activeTime = now
	}

	ledger := DidLedger{
		DocType:          DocTypeDid,
		DidNo:            req.DidNo,
		DidValue:         req.DidValue,
		AccountNo:        req.AccountNo,
		SubjectType:      req.SubjectType,
		Status:           StatusActive,
		ActiveTime:       activeTime,
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

	emitEvent(ctx, "DidRegistered", "DID", req.DidNo, StatusActive)
	return nil
}

// SuspendDID 挂起DID
func (c *DidContract) SuspendDID(ctx contractapi.TransactionContextInterface, payloadJSON string) error {
	var req SuspendDIDRequest
	if err := json.Unmarshal([]byte(payloadJSON), &req); err != nil {
		return fmt.Errorf("invalid payload: %w", err)
	}
	if req.DidNo == "" {
		return fmt.Errorf("didNo is required")
	}

	key := ledgerKey(req.DidNo)
	b, err := ctx.GetStub().GetState(key)
	if err != nil {
		return fmt.Errorf("failed to read ledger: %w", err)
	}
	if b == nil {
		return fmt.Errorf("DID not found: %s", req.DidNo)
	}

	var ledger DidLedger
	if err = json.Unmarshal(b, &ledger); err != nil {
		return fmt.Errorf("failed to unmarshal ledger: %w", err)
	}
	if ledger.Status != StatusActive {
		return fmt.Errorf("DID is not active, current status: %s", ledger.Status)
	}

	now := txTime(ctx)
	ledger.Status = StatusSuspended
	ledger.SuspendTime = req.SuspendTime
	if ledger.SuspendTime == "" {
		ledger.SuspendTime = now
	}
	ledger.Reason = req.Reason
	ledger.TxTime = now

	updated, err := json.Marshal(ledger)
	if err != nil {
		return fmt.Errorf("failed to marshal ledger: %w", err)
	}
	if err = ctx.GetStub().PutState(key, updated); err != nil {
		return fmt.Errorf("failed to put state: %w", err)
	}

	emitEvent(ctx, "DidSuspended", "DID", req.DidNo, StatusSuspended)
	return nil
}

// RevokeDID 吊销DID
func (c *DidContract) RevokeDID(ctx contractapi.TransactionContextInterface, payloadJSON string) error {
	var req RevokeDIDRequest
	if err := json.Unmarshal([]byte(payloadJSON), &req); err != nil {
		return fmt.Errorf("invalid payload: %w", err)
	}
	if req.DidNo == "" {
		return fmt.Errorf("didNo is required")
	}

	key := ledgerKey(req.DidNo)
	b, err := ctx.GetStub().GetState(key)
	if err != nil {
		return fmt.Errorf("failed to read ledger: %w", err)
	}
	if b == nil {
		return fmt.Errorf("DID not found: %s", req.DidNo)
	}

	var ledger DidLedger
	if err = json.Unmarshal(b, &ledger); err != nil {
		return fmt.Errorf("failed to marshal ledger: %w", err)
	}
	if ledger.Status == StatusRevoked {
		return fmt.Errorf("DID is already revoked: %s", req.DidNo)
	}

	now := txTime(ctx)
	ledger.Status = StatusRevoked
	ledger.RevokeTime = req.RevokeTime
	if ledger.RevokeTime == "" {
		ledger.RevokeTime = now
	}
	ledger.Reason = req.Reason
	ledger.TxTime = now

	updated, err := json.Marshal(ledger)
	if err != nil {
		return fmt.Errorf("failed to marshal ledger: %w", err)
	}
	if err = ctx.GetStub().PutState(key, updated); err != nil {
		return fmt.Errorf("failed to put state: %w", err)
	}

	emitEvent(ctx, "DidRevoked", "DID", req.DidNo, StatusRevoked)
	return nil
}

// QueryDID 查询DID账本状态
func (c *DidContract) QueryDID(ctx contractapi.TransactionContextInterface, didNo string) (*DidLedger, error) {
	key := ledgerKey(didNo)
	b, err := ctx.GetStub().GetState(key)
	if err != nil {
		return nil, fmt.Errorf("failed to read ledger: %w", err)
	}
	if b == nil {
		return nil, fmt.Errorf("DID not found: %s", didNo)
	}
	var ledger DidLedger
	if err = json.Unmarshal(b, &ledger); err != nil {
		return nil, fmt.Errorf("failed to unmarshal ledger: %w", err)
	}
	return &ledger, nil
}

// GetHistoryByKey 查询DID的完整历史记录
func (c *DidContract) GetHistoryByKey(ctx contractapi.TransactionContextInterface, didNo string) (string, error) {
	key := ledgerKey(didNo)
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
