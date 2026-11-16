package com.marknote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("image")
public class Image {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String url;

    private String bucket;

    private String objectName;

    private LocalDateTime createTime;
}
