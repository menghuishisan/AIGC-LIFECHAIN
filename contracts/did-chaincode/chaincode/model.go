package chaincode

// DidLedger 是 DID 的账本存储对象
// Key: DID#{didNo}
type DidLedger struct {
	DocType          string `json:"docType"`
	DidNo            string `json:"didNo"`
	DidValue         string `json:"didValue"`
	AccountNo        string `json:"accountNo"`
	SubjectType      string `json:"subjectType"`
	Status           string `json:"status"`
	ActiveTime       string `json:"activeTime"`
	SuspendTime      string `json:"suspendTime,omitempty" metadata:",optional"`
	RevokeTime       string `json:"revokeTime,omitempty" metadata:",optional"`
	Reason           string `json:"reason,omitempty" metadata:",optional"`
	TxTime           string `json:"txTime"`
	ChaincodeVersion string `json:"chaincodeVersion"`
}

// RegisterDIDRequest 注册DID的请求体
type RegisterDIDRequest struct {
	DidNo       string `json:"didNo"`
	DidValue    string `json:"didValue"`
	AccountNo   string `json:"accountNo"`
	SubjectType string `json:"subjectType"`
	ActiveTime  string `json:"activeTime"`
}

// SuspendDIDRequest 挂起DID的请求体
type SuspendDIDRequest struct {
	DidNo       string `json:"didNo"`
	Reason      string `json:"reason"`
	SuspendTime string `json:"suspendTime"`
}

// RevokeDIDRequest 吊销DID的请求体
type RevokeDIDRequest struct {
	DidNo      string `json:"didNo"`
	Reason     string `json:"reason"`
	RevokeTime string `json:"revokeTime"`
}

// EventPayload 链码事件负载
type EventPayload struct {
	BizType string `json:"bizType"`
	BizNo   string `json:"bizNo"`
	TxTime  string `json:"txTime"`
	Status  string `json:"status"`
}
