package com.lifechain.infra.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对象存储元数据
 * <p>
 * 统一表达对象存储中文件的关键信息，便于业务层在上传回调、
 * 受控下载等场景下基于服务端真实元数据做校验。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageObjectMetadata {

    /** 对象名 */
    private String objectName;

    /** 文件大小，单位字节 */
    private long size;

    /** MIME 类型 */
    private String contentType;

    /** ETag 或对象版本摘要 */
    private String etag;
}
