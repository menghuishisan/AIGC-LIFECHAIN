package chaincode

// SettlementLedger 结算账本存储对象
// Key: SETTLE#{settleNo}
type SettlementLedger struct {
	DocType          string `json:"docType"`
	SettleNo         string `json:"settleNo"`
	OrderNo          string `json:"orderNo"`
	TotalAmount      int64  `json:"totalAmount"`
	SummaryHash      string `json:"summaryHash"`
	Status           string `json:"status"`
	SettleTime       string `json:"settleTime"`
	TxTime           string `json:"txTime"`
	ChaincodeVersion string `json:"chaincodeVersion"`
}

// ReverseSettlementLedger 逆分账账本存储对象
// Key: REVERSE_SETTLE#{reverseNo}
type ReverseSettlementLedger struct {
	DocType          string `json:"docType"`
	ReverseNo        string `json:"reverseNo"`
	SettleNo         string `json:"settleNo"`
	ReverseAmount    int64  `json:"reverseAmount"`
	Reason           string `json:"reason"`
	Status           string `json:"status"`
	ReverseTime      string `json:"reverseTime"`
	TxTime           string `json:"txTime"`
	ChaincodeVersion string `json:"chaincodeVersion"`
}

// RegisterSettlementRequest 注册结算的请求体
type RegisterSettlementRequest struct {
	SettleNo    string `json:"settleNo"`
	OrderNo     string `json:"orderNo"`
	TotalAmount int64  `json:"totalAmount"`
	SummaryHash string `json:"summaryHash"`
	SettleTime  string `json:"settleTime"`
}

// RegisterReverseSettlementRequest 注册逆分账的请求体
// 字段与 SettlementChainAdapterImpl.registerReverseSettlement 完全对齐
type RegisterReverseSettlementRequest struct {
	ReverseNo     string `json:"reverseNo"`
	SettleNo      string `json:"settleNo"`
	ReverseAmount int64  `json:"reverseAmount"`
	Reason        string `json:"reason"`
	ReverseTime   string `json:"reverseTime"`
}

// EventPayload 链码事件负载
type EventPayload struct {
	BizType string `json:"bizType"`
	BizNo   string `json:"bizNo"`
	TxTime  string `json:"txTime"`
	Status  string `json:"status"`
}
