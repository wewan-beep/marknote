package com.marknote.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marknote.entity.Image;
import com.marknote.mapper.ImageMapper;
import com.marknote.service.ImageService;
import org.springframework.stereotype.Service;

@Service
public class ImageServiceImpl extends ServiceImpl<ImageMapper, Image> implements ImageService {
}
