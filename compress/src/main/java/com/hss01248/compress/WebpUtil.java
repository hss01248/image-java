package com.hss01248.compress;

import cn.hutool.log.StaticLog;
import com.hss01248.quality.Magick;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.nio.AnimatedGif;
import com.sksamuel.scrimage.nio.AnimatedGifReader;
import com.sksamuel.scrimage.nio.ImageSource;
import com.sksamuel.scrimage.webp.Gif2WebpWriter;
import com.sksamuel.scrimage.webp.WebpWriter;

import java.io.File;
import java.nio.file.Path;

/**
 * @author: hss01248
 * @date: 2023/1/2
 * @desc: //todo
 */
public class WebpUtil {
    public static void main(String[] args) {

        String dir = "/Users/hss/aku2/xiaomi/path1/test";
        File dirs = new File(dir);
        StaticLog.warn("dir can read: "+ dirs.canRead());
        StaticLog.warn("dir exist: "+ dirs.exists());
        File[] list = dirs.listFiles();
        if(list == null){
            StaticLog.warn("list == null");
            return;
        }
        if(list.length ==0){
            StaticLog.warn("list.length ==0");
            return;
        }
        for (File s : list) {
            try {
                toWebP(s.getAbsolutePath());
                //gifToWebP(s.getAbsolutePath());
            } catch (Exception e) {
                System.out.println(s);
                e.printStackTrace();
            }
        }

    }

    /**
     * 尼康相机拍照的压缩:
     * E:\photos\编年史\2022\20220305小区内\DSCN1743.JPG -> DSCN1743.webp
     * 2695kB->990kB,缩小:63%, 耗时:10954ms
     * 原图jpg质量: 86
     * @param path
     * @return
     * @throws Exception
     */
    public static File toWebP(String path) throws Exception{
        long start = System.currentTimeMillis();
        File file = new File(path);
        ImmutableImage image = ImmutableImage.loader().fromFile(file);

        String name = file.getName().substring(0,file.getName().lastIndexOf(".")+1)+"webp";
        File newPath = new File(file.getParent(),name);

        Path output = image.output(WebpWriter.DEFAULT, newPath.getAbsolutePath());
        String desc = file.getAbsolutePath()+" -> "+name+"\n"+(file.length()/1024)+"kB->"+(newPath.length()/1024)+"kB,缩小:"+
                (file.length()-newPath.length())*100/file.length()+"%, 耗时:"+(System.currentTimeMillis()- start)+"ms";
        System.out.println(desc);
        int quality = new Magick().getJPEGImageQuality(file);
        System.out.println("原图jpg质量: "+ quality);
        return newPath;
    }

    public static File gifToWebP(String path) throws Exception{
        long start = System.currentTimeMillis();
        File file = new File(path);
        AnimatedGif gif = AnimatedGifReader.read(ImageSource.of(file));

        String name = file.getName().substring(0,file.getName().lastIndexOf(".")+1)+"webp";
        File newPath = new File(file.getParent(),name);

        Path output = gif.output(Gif2WebpWriter.DEFAULT, newPath.getAbsolutePath());
        String desc = file.getAbsolutePath()+" -> "+name+"\n"+(file.length()/1024)+"kB->"+(newPath.length()/1024)+"kB,缩小:"+
                (file.length()-newPath.length())*100/file.length()+"%, 耗时:"+(System.currentTimeMillis()- start)+"ms";
        System.out.println(desc);
        return newPath;
    }
}
