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

	now := txTime(ctx)
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

	emitEvent(ctx, "ClaimRegistered", "CLAIM", req.ClaimNo, StatusSuccess)
	return nil
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
