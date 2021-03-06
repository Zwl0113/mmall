package com.mmall.service.impl;

import com.google.common.collect.Lists;
import com.mmall.service.IFileService;
import com.mmall.util.FTPUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service("iFileService")
@Slf4j
public class FileServiceImpl implements IFileService {

    public String upload(MultipartFile file,String path){
        String originalFilename = file.getOriginalFilename();
        String fileExtensionName = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        String uploadFileName = UUID.randomUUID().toString()+"."+fileExtensionName;
        log.info("开始上传文件，文件的格式为，上传的文件名{}，上传路径{}，新文件名{}",originalFilename,path,uploadFileName);

        File fileDir = new File(path);
        if(!fileDir.exists()){
            fileDir.setWritable(true);
            fileDir.mkdirs();
        }
        File targetFile = new File(path,uploadFileName);
        try {
            file.transferTo(targetFile);
            //文件已经上传成功了
            FTPUtil.uploadFile(Lists.newArrayList(targetFile));
            //已经上传到FTP服务器上

            targetFile.delete();
        } catch (IOException e) {
            log.info("文件上传失败",e);
            return null;
        }
        return targetFile.getName();
    }
}
