package chaincode

// FreezeLedger 冻结账本存储对象
// Key: FREEZE#{freezeNo}
type FreezeLedger struct {
	DocType          string `json:"docType"`
	FreezeNo         string `json:"freezeNo"`
	TargetType       string `json:"targetType"`
	TargetNo         string `json:"targetNo"`
	Reason           string `json:"reason"`
	Status           string `json:"status"`
	FreezeTime       string `json:"freezeTime"`
	UnfreezeTime     string `json:"unfreezeTime,omitempty" metadata:",optional"`
	UnfreezeReason   string `json:"unfreezeReason,omitempty" metadata:",optional"`
	TxTime           string `json:"txTime"`
	ChaincodeVersion string `json:"chaincodeVersion"`
}

// DisputeLedger 争议结论账本存储对象
// Key: DISPUTE#{caseNo}
type DisputeLedger struct {
	DocType          string `json:"docType"`
	CaseNo           string `json:"caseNo"`
	Conclusion       string `json:"conclusion"`
	ResultSummary    string `json:"resultSummary"`
	Status           string `json:"status"`
	CloseTime        string `json:"closeTime"`
	TxTime           string `json:"txTime"`
	ChaincodeVersion string `json:"chaincodeVersion"`
}

// ReportLedger 监管报告账本存储对象
// Key: REPORT#{reportNo}
type ReportLedger struct {
	DocType          string `json:"docType"`
	ReportNo         string `json:"reportNo"`
	SummaryHash      string `json:"summaryHash"`
	Status           string `json:"status"`
	GenerateTime     string `json:"generateTime"`
	TxTime           string `json:"txTime"`
	ChaincodeVersion string `json:"chaincodeVersion"`
}

// RegisterFreezeRequest 注册冻结的请求体（与后端 RegulatoryChainAdapterImpl 对齐）
type RegisterFreezeRequest struct {
	FreezeNo   string `json:"freezeNo"`
	TargetType string `json:"targetType"`
	TargetNo   string `json:"targetNo"`
	Reason     string `json:"reason"`
	FreezeTime string `json:"freezeTime"`
}

// RegisterUnfreezeRequest 注册解冻的请求体（与后端 RegulatoryChainAdapterImpl 对齐）
type RegisterUnfreezeRequest struct {
	FreezeNo     string `json:"freezeNo"`
	Reason       string `json:"reason"`
	UnfreezeTime string `json:"unfreezeTime"`
}

// RegisterDisputeConclusionRequest 注册争议结论的请求体（与后端 RegulatoryChainAdapterImpl 对齐）
type RegisterDisputeConclusionRequest struct {
	CaseNo        string `json:"caseNo"`
	Conclusion    string `json:"conclusion"`
	ResultSummary string `json:"resultSummary"`
	CloseTime     string `json:"closeTime"`
}

// RegisterReportRequest 注册监管报告的请求体（与后端 RegulatoryChainAdapterImpl 对齐）
type RegisterReportRequest struct {
	ReportNo     string `json:"reportNo"`
	SummaryHash  string `json:"summaryHash"`
	GenerateTime string `json:"generateTime"`
}

// EventPayload 链码事件负载
type EventPayload struct {
	BizType string `json:"bizType"`
	BizNo   string `json:"bizNo"`
	TxTime  string `json:"txTime"`
	Status  string `json:"status"`
}
