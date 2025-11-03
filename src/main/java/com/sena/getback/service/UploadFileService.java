package com.sena.getback.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadFileService {

    private final String folder = "images/";

    public UploadFileService() {
        File directorio = new File(folder);
        if (!directorio.exists()) {
            directorio.mkdirs();
        }
    }

    // Guardar imagen en carpeta /images
    public String saveImages(MultipartFile file, String nombre) throws IOException {
        if (file != null && !file.isEmpty()) {
            byte[] bytes = file.getBytes();

            // nombre único
            String fileName = System.currentTimeMillis() + "_" 
                            + nombre.replace(" ", "_") + "_" 
                            + file.getOriginalFilename();

            Path path = Paths.get(folder + fileName);
            Files.write(path, bytes);

            return fileName;
        }
        return "default.jpg";
    }

    // Eliminar imagen
    public void deleteImage(String nombre) {
        if (nombre != null && !nombre.equals("default.jpg")) {
            File file = new File(folder + nombre);
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
