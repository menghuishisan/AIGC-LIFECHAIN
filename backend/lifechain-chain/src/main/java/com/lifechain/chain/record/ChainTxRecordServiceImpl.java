package com.lifechain.chain.record;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifechain.chain.model.ChainSubmitRequest;
import com.lifechain.chain.model.ChainSubmitResult;
import com.lifechain.common.enums.ChainStatusEnum;
import com.lifechain.common.mq.ChainCompensationMessage;
import com.lifechain.infra.mq.MessagePublisher;
import com.lifechain.infra.mq.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 区块链交易记录服务实现
 * <p>
 * 使用 MyBatis-Plus 操作 {@code chain_tx_record} 表，完成交易记录的增删改查。
 * 所有写操作均记录日志，便于问题追溯。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChainTxRecordServiceImpl implements ChainTxRecordService {

    private final ChainTxRecordMapper chainTxRecordMapper;
    private final MessagePublisher messagePublisher;

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveRecord(ChainSubmitRequest request, ChainSubmitResult result) {
        ChainTxRecordEntity entity = new ChainTxRecordEntity();
        entity.setBizType(request.getBizType());
        entity.setBizId(request.getBizId());
        entity.setBizNo(request.getBizNo());
        entity.setTxType(request.getTxType());
        entity.setChannelName(result.getChannelName());
        entity.setChaincodeName(request.getChaincodeName());
        entity.setTxHash(result.getTxHash());
        entity.setBlockHeight(result.getBlockHeight());
        entity.setRequestPayloadHash(request.getRequestPayloadHash());
        entity.setResponsePayload(result.getResponsePayload());
        entity.setEndorsementSummary(result.getEndorsementSummary());
        entity.setSubmitTime(result.getSubmitTime());
        entity.setConfirmTime(result.getConfirmTime());

        if (result.isSuccess()) {
            entity.setChainStatus(ChainStatusEnum.CHAIN_SUBMITTED.getCode());
        } else {
            entity.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
            entity.setFailReason(result.getFailReason());
            entity.setReasonCode(result.getReasonCode());
        }

        chainTxRecordMapper.insert(entity);
        log.info("链上交易记录已保存，bizType={}, bizNo={}, txHash={}, chainStatus={}",
                entity.getBizType(), entity.getBizNo(), entity.getTxHash(), entity.getChainStatus());

        if (ChainStatusEnum.CHAIN_SUBMITTED.getCode().equals(entity.getChainStatus())) {
            messagePublisher.sendDelayed(RabbitMQConfig.RK_CHAIN_COMPENSATION_DELAY,
                    new ChainCompensationMessage(entity.getId(), entity.getBizType(), entity.getBizNo()),
                    5 * 60 * 1000L);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainTxRecordEntity getByBizTypeAndBizNo(String bizType, String bizNo) {
        LambdaQueryWrapper<ChainTxRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChainTxRecordEntity::getBizType, bizType)
                .eq(ChainTxRecordEntity::getBizNo, bizNo)
                .orderByDesc(ChainTxRecordEntity::getCreatedAt)
                .last("LIMIT 1");
        return chainTxRecordMapper.selectOne(wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainTxRecordEntity getByTxHash(String txHash) {
        LambdaQueryWrapper<ChainTxRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChainTxRecordEntity::getTxHash, txHash);
        return chainTxRecordMapper.selectOne(wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChainTxRecordEntity> listByBizTypeAndBizId(String bizType, Long bizId) {
        LambdaQueryWrapper<ChainTxRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChainTxRecordEntity::getBizType, bizType)
                .eq(ChainTxRecordEntity::getBizId, bizId)
                .orderByDesc(ChainTxRecordEntity::getCreatedAt);
        return chainTxRecordMapper.selectList(wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChainTxRecordEntity> listByBizTypeAndBizNo(String bizType, String bizNo) {
        LambdaQueryWrapper<ChainTxRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChainTxRecordEntity::getBizType, bizType)
                .eq(ChainTxRecordEntity::getBizNo, bizNo)
                .orderByDesc(ChainTxRecordEntity::getSubmitTime);
        return chainTxRecordMapper.selectList(wrapper);
    }

}
