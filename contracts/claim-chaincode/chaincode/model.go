package chaincode

// ClaimLedger 确权申请账本存储对象
// Key: CLAIM#{claimNo}
type ClaimLedger struct {
	DocType          string `json:"docType"`
	ClaimNo          string `json:"claimNo"`
	WorkNo           string `json:"workNo"`
	CreatorDid       string `json:"creatorDid"`
	FileHash         string `json:"fileHash"`
	MetaHash         string `json:"metaHash"`
	SummaryHash      string `json:"summaryHash"`
	Status           string `json:"status"`
	ClaimTime        string `json:"claimTime"`
	TxTime           string `json:"txTime"`
	ChaincodeVersion string `json:"chaincodeVersion"`
}

// RegisterClaimRequest 注册确权的请求体
type RegisterClaimRequest struct {
	ClaimNo     string `json:"claimNo"`
	WorkNo      string `json:"workNo"`
	CreatorDid  string `json:"creatorDid"`
	FileHash    string `json:"fileHash"`
	MetaHash    string `json:"metaHash"`
	SummaryHash string `json:"summaryHash"`
	ClaimTime   string `json:"claimTime"`
}

// EventPayload 链码事件负载
type EventPayload struct {
	BizType string `json:"bizType"`
	BizNo   string `json:"bizNo"`
	TxTime  string `json:"txTime"`
	Status  string `json:"status"`
}
