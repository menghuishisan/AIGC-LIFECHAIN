package chaincode

import (
	"encoding/json"
	"fmt"
	"time"

	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

const (
	ChaincodeVersion = "v1"
	DocTypeClaim     = "claim"
	StatusSuccess    = "CLAIM_SUCCESS"
)

// ClaimContract 实现确权链码的所有合约函数
type ClaimContract struct {
	contractapi.Contract
}

func ledgerKey(claimNo string) string {
	return fmt.Sprintf("CLAIM#%s", claimNo)
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

// RegisterClaim 注册确权记录到账本
// 幂等：同一 claimNo 已存在且载荷相同则幂等成功；载荷不同则返回冲突错误
func (c *ClaimContract) RegisterClaim(ctx contractapi.TransactionContextInterface, payloadJSON string) error {
	var req RegisterClaimRequest
	if err := json.Unmarshal([]byte(payloadJSON), &req); err != nil {
		return fmt.Errorf("invalid payload: %w", err)
	}
	if req.ClaimNo == "" || req.WorkNo == "" || req.CreatorDid == "" {
		return fmt.Errorf("claimNo, workNo, creatorDid are required")
	}
	if req.FileHash == "" || req.MetaHash == "" || req.SummaryHash == "" {
		return fmt.Errorf("fileHash, metaHash, summaryHash are required")
	}

	key := ledgerKey(req.ClaimNo)
	existing, err := ctx.GetStub().GetState(key)
	if err != nil {
		return fmt.Errorf("failed to read ledger: %w", err)
	}
	if existing != nil {
		var existingLedger ClaimLedger
		if err = json.Unmarshal(existing, &existingLedger); err != nil {
			return fmt.Errorf("failed to unmarshal existing ledger: %w", err)
		}
		if existingLedger.WorkNo == req.WorkNo && existingLedger.CreatorDid == req.CreatorDid &&
			existingLedger.FileHash == req.FileHash && existingLedger.MetaHash == req.MetaHash &&
			existingLedger.SummaryHash == req.SummaryHash {
			return nil
		}
		return fmt.Errorf("CONFLICT: claim already exists with different payload: %s", req.ClaimNo)
	}

	now, err := txTime(ctx)
	if err != nil {
		return err
	}
	claimTime := req.ClaimTime
	if claimTime == "" {
		claimTime = now
	}

	ledger := ClaimLedger{
		DocType:          DocTypeClaim,
		ClaimNo:          req.ClaimNo,
		WorkNo:           req.WorkNo,
		CreatorDid:       req.CreatorDid,
		FileHash:         req.FileHash,
		MetaHash:         req.MetaHash,
		SummaryHash:      req.SummaryHash,
		Status:           StatusSuccess,
		ClaimTime:        claimTime,
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

	return emitEvent(ctx, "ClaimRegistered", "CLAIM", req.ClaimNo, StatusSuccess)
}

// QueryClaim 查询确权账本状态
func (c *ClaimContract) QueryClaim(ctx contractapi.TransactionContextInterface, claimNo string) (*ClaimLedger, error) {
	key := ledgerKey(claimNo)
	b, err := ctx.GetStub().GetState(key)
	if err != nil {
		return nil, fmt.Errorf("failed to read ledger: %w", err)
	}
	if b == nil {
		return nil, fmt.Errorf("claim not found: %s", claimNo)
	}
	var ledger ClaimLedger
	if err = json.Unmarshal(b, &ledger); err != nil {
		return nil, fmt.Errorf("failed to unmarshal ledger: %w", err)
	}
	return &ledger, nil
}

// GetHistoryByKey 查询确权的完整历史记录
func (c *ClaimContract) GetHistoryByKey(ctx contractapi.TransactionContextInterface, claimNo string) (string, error) {
	key := ledgerKey(claimNo)
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
