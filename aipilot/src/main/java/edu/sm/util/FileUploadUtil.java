package edu.sm.util;


import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUploadUtil {
    public  static void deleteFile(String filename, String dir) throws IOException {
        Path filePath = Paths.get(dir+filename);
        Files.delete(filePath);
    }
    public  static void saveFile(MultipartFile mf, String dir) throws IOException {
        byte [] data;
        String imgname = mf.getOriginalFilename();
        try {
            data = mf.getBytes();
            FileOutputStream fo =
                    new FileOutputStream(dir+imgname);
            fo.write(data);
            fo.close();
        }catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}