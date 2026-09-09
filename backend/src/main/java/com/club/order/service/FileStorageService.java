package com.club.order.service;

import org.springframework.web.multipart.MultipartFile;

/** 图片存储抽象。当前用本地文件存储，上架时替换为 COS/OSS，调用方不变。 */
public interface FileStorageService {
    /** 保存文件并返回可访问的相对 URL，例如 /uploads/xxx.jpg。 */
    String store(MultipartFile file);
}
