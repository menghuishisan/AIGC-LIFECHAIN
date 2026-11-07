.PHONY: all build tidy test vet clean

CHAINCODES := did-chaincode claim-chaincode license-chaincode settlement-chaincode regulatory-chaincode

all: build

build: $(addprefix build-,$(CHAINCODES))

build-%:
	@echo ">>> Building $*..."
	cd $* && go build -v ./...

tidy:
	@set -e; for cc in $(CHAINCODES); do \
		echo ">>> go mod tidy: $$cc"; \
		(cd $$cc && go mod tidy); \
	done

test:
	@set -e; for cc in $(CHAINCODES); do \
		echo ">>> Testing $$cc"; \
		(cd $$cc && go test ./...); \
	done

vet:
	@set -e; for cc in $(CHAINCODES); do \
		echo ">>> go vet: $$cc"; \
		(cd $$cc && go vet ./...); \
	done

clean:
	@for cc in $(CHAINCODES); do \
		rm -f $$cc/$$cc; \
	done

# 打包为 Fabric 链码包（需要 peer CLI）
package-%:
	cd $* && peer lifecycle chaincode package $*.tar.gz \
		--path . --lang golang --label $*_1.0
