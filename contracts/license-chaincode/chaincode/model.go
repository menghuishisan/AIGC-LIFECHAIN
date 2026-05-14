package chaincode

// LicenseLedger 授权许可账本存储对象
// Key: LICENSE#{licenseNo}
type LicenseLedger struct {
	DocType          string `json:"docType"`
	LicenseNo        string `json:"licenseNo"`
	WorkNo           string `json:"workNo"`
	LicensorDid      string `json:"licensorDid"`
	LicenseeDid      string `json:"licenseeDid"`
	LicenseType      string `json:"licenseType"`
	LicenseHash      string `json:"licenseHash"`
	Status           string `json:"status"`
	EffectiveTime    string `json:"effectiveTime"`
	TxTime           string `json:"txTime"`
	ChaincodeVersion string `json:"chaincodeVersion"`
}

// RegisterLicenseRequest 注册授权的请求体
type RegisterLicenseRequest struct {
	LicenseNo     string `json:"licenseNo"`
	WorkNo        string `json:"workNo"`
	LicensorDid   string `json:"licensorDid"`
	LicenseeDid   string `json:"licenseeDid"`
	LicenseType   string `json:"licenseType"`
	LicenseHash   string `json:"licenseHash"`
	EffectiveTime string `json:"effectiveTime"`
}

// EventPayload 链码事件负载
type EventPayload struct {
	BizType string `json:"bizType"`
	BizNo   string `json:"bizNo"`
	TxTime  string `json:"txTime"`
	Status  string `json:"status"`
}
