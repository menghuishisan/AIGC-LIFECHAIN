package chaincode

import (
	"encoding/json"
	"fmt"
	"time"

	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

const (
	ChaincodeVersion = "v1"
	DocTypeLicense   = "license"
	StatusSuccess    = "LICENSE_ACTIVE"
)

// LicenseContract 实现授权许可链码的所有合约函数
type LicenseContract struct {
	contractapi.Contract
}

func ledgerKey(licenseNo string) string {
	return fmt.Sprintf("LICENSE#%s", licenseNo)
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

// RegisterLicense 注册授权许可到账本
// 幂等：同一 licenseNo 已存在且载荷相同则幂等成功；载荷不同则返回冲突错误
func (c *LicenseContract) RegisterLicense(ctx contractapi.TransactionContextInterface, payloadJSON string) error {
	var req RegisterLicenseRequest
	if err := json.Unmarshal([]byte(payloadJSON), &req); err != nil {
		return fmt.Errorf("invalid payload: %w", err)
	}
	if req.LicenseNo == "" || req.WorkNo == "" || req.LicensorDid == "" || req.LicenseeDid == "" {
		return fmt.Errorf("licenseNo, workNo, licensorDid, licenseeDid are required")
	}
	if req.LicenseHash == "" {
		return fmt.Errorf("licenseHash is required")
	}

	key := ledgerKey(req.LicenseNo)
	existing, err := ctx.GetStub().GetState(key)
	if err != nil {
		return fmt.Errorf("failed to read ledger: %w", err)
	}
	if existing != nil {
		var existingLedger LicenseLedger
		if err = json.Unmarshal(existing, &existingLedger); err != nil {
			return fmt.Errorf("failed to unmarshal existing ledger: %w", err)
		}
		if existingLedger.WorkNo == req.WorkNo && existingLedger.LicensorDid == req.LicensorDid &&
			existingLedger.LicenseeDid == req.LicenseeDid && existingLedger.LicenseHash == req.LicenseHash {
			return nil
		}
		return fmt.Errorf("CONFLICT: license already exists with different payload: %s", req.LicenseNo)
	}

	now := txTime(ctx)
	effectiveTime := req.EffectiveTime
	if effectiveTime == "" {
		effectiveTime = now
	}

	ledger := LicenseLedger{
		DocType:          DocTypeLicense,
		LicenseNo:        req.LicenseNo,
		WorkNo:           req.WorkNo,
		LicensorDid:      req.LicensorDid,
		LicenseeDid:      req.LicenseeDid,
		LicenseType:      req.LicenseType,
		LicenseHash:      req.LicenseHash,
		Status:           StatusSuccess,
		EffectiveTime:    effectiveTime,
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

	emitEvent(ctx, "LicenseRegistered", "LICENSE", req.LicenseNo, StatusSuccess)
	return nil
}

// QueryLicense 查询授权许可账本状态
func (c *LicenseContract) QueryLicense(ctx contractapi.TransactionContextInterface, licenseNo string) (*LicenseLedger, error) {
	key := ledgerKey(licenseNo)
	b, err := ctx.GetStub().GetState(key)
	if err != nil {
		return nil, fmt.Errorf("failed to read ledger: %w", err)
	}
	if b == nil {
		return nil, fmt.Errorf("license not found: %s", licenseNo)
	}
	var ledger LicenseLedger
	if err = json.Unmarshal(b, &ledger); err != nil {
		return nil, fmt.Errorf("failed to unmarshal ledger: %w", err)
	}
	return &ledger, nil
}

// GetHistoryByKey 查询授权许可的完整历史记录
func (c *LicenseContract) GetHistoryByKey(ctx contractapi.TransactionContextInterface, licenseNo string) (string, error) {
	key := ledgerKey(licenseNo)
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
