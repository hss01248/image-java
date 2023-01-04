package com.hss01248.compress;

import cn.hutool.core.io.FileUtil;
import com.hss01248.quality.Magick;
import net.coobird.thumbnailator.Thumbnails;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class ImgCompressor {

//https://www.appinn.com/photoprism/
    public static void main(String[] args){
        String path = "/Users/hss/aku2\\xiaomi\\path1\\test";
        ThumbnailUtil.cacheRoot = "/Users/hss/aku2\\xiaomi\\path1\\cache";


        BaseBean baseBean = compress(path);
        System.out.println(baseBean.msg);
         ThumbnailUtil.generateThumbs(path);

        //String path2 = "D:\\Snipaste_2022-12-10_20-42-06-2.jpg";
        //printJpgQuality(path2);

    }

    public static void printJpgQuality(String path){
        int jpegImageQuality = new Magick().getJPEGImageQuality(new File(path));
        System.out.println("jpg quality: "+ jpegImageQuality+", "+ path);
    }



    public static BaseBean compress(String path){
        File file = new File(path);
        if(!file.exists()){
            return BaseBean.error("404","file not exist");
        }
        if(file.isFile()){
            return compressSingleImage(path);
        }
        System.out.println("----------------------> 开始压缩文件夹内图片: "+path);
        //if(file.isDirectory()){
            File[] files = file.listFiles();
            if(files == null || files.length==0){
                return BaseBean.error("empty","dir is empty : "+ path);
            }
            String msg = "文件共"+files.length+"个";
            int successCount = 0;
            long totalSize = 0;
            long sizeAfterCompressed = 0;
            List<File> subFolders = new ArrayList<>();
            for (File file1 : files) {
                if(file1.isDirectory()){
                    subFolders.add(file1);
                    System.out.println("子文件夹,添加到待压缩列表: "+file1.getAbsolutePath());
                    continue;
                }
                totalSize +=file1.length();
                BaseBean baseBean = compressSingleImage(file1.getAbsolutePath());
                if(baseBean.success){
                    successCount++;
                    sizeAfterCompressed +=file1.length();
                }else {
                    totalSize -=file1.length();
                    System.out.println(baseBean.msg+" :"+file1.getAbsolutePath());
                }
            }
            msg+=",其中文件夹个数:"+subFolders.size();
            msg+= ", 压缩了"+successCount+"个,其余无需压缩,节约空间:"+ Math.round((totalSize - sizeAfterCompressed)/1024f/1024)+"MB";
            deleteFile(file);
            System.out.println("done ----------------------> 当前文件夹压缩情况: "+msg);
            if(!subFolders.isEmpty()){
                for (File subFolder : subFolders) {
                    compress(subFolder.getAbsolutePath());
                }
            }

            return BaseBean.successBack().setMsg(msg);
        //}
    }

    static String formatFileSize(long size){
        if(size < 1024){
            return size+"B";
        }else if (size < 1024 * 1024){
            return size/1024f+"kB";
        }else if (size < 1024 * 1024 * 1024){
            return size/1024f/1024+"MB";
        }else {
            return size/1024f/1024/1024+"GB";
        }
    }

    private static void deleteFile(File file) {
        if(file.isDirectory()){
            File[] files = file.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return s.endsWith("_original");
                }
            });
            if(files != null && files.length > 0){
                for (File file1 : files) {
                    file1.delete();
                }
            }
        }
    }

    public static BaseBean compressSingleImage(String path){
        System.out.println("to compress path: "+path);
        long start = System.currentTimeMillis();

        File file = new File(path);
        if(!file.exists()){
            return BaseBean.error("404","file not exist");
        }

        //判断压缩文件or文件夹

        //只压缩jpg,png
        String mime = FileUtil.getMimeType(path);
        if(path.endsWith(".JPG")){
            mime = "image/jpeg";
        }

        if(!"image/jpeg".equals(mime) && !"image/png".equals(mime) ){
            return BaseBean.error("no need","this type do not need to compress : "+mime);
        }
        if(file.length() < 102400){
            //100k以下的不压缩
              return BaseBean.error("no need","ignore file below 100kB : "+file.length()/1024/1024+"MB");
        }
        if(mime.equals("image/jpeg")){
            int jpgQuality = new Magick().getJPEGImageQuality(file);
            System.out.println("jpg, 质量为: "+jpgQuality+", 目标:85");
            if(jpgQuality>0 && jpgQuality < 90){
                return BaseBean.error("no need","jpeg quality is already ok, no need to compress :"+ jpgQuality);
            }
        }

        File compressedFile = new File(file.getParent(),file.getName()+".jpg");

        try {
            Thumbnails.of(file)
                    .scale(1.0d)
                     //.size(640, 480)
                    .outputQuality(0.85f)
                    .outputFormat("jpg")
                    .useExifOrientation(true)
                    .toFile(compressedFile);
            if(!compressedFile.exists() || compressedFile.length() ==0){
                compressedFile.delete();
                return BaseBean.error("compress fail","compressedFile not exists or compressedFile.length() ==0");
            }

            if(compressedFile.length() > file.length()){
                compressedFile.delete();
                return BaseBean.error("bigger","压缩后反而变大");
            }
            System.out.println("压缩成功 ");
            if(mime.equals("image/jpeg")){
                System.out.println("jpg, 准备写exif: "+file.getAbsolutePath());
                //todo 使用exiftool拷贝exif  复制：exiftool [OPTIONS] -tagsFromFile SRCFILE [-SRCTAG[>DSTTAG]...] FILE...
                String exifToolPath = getToolPath();
                String cmd = exifToolPath + " -tagsFromFile "+file.getAbsolutePath()+" -orientation#=1 "+ compressedFile.getAbsolutePath();
                Process exec = Runtime.getRuntime().exec(cmd);

                //exiftool内部是异步的
                boolean wait = true;
                int waitTime = 0;
                File tmp = new File(compressedFile.getParentFile(),compressedFile.getName()+"_original");
                while (wait){
                    waitTime += 100;
                    Thread.sleep(100);
                    if(tmp.exists()){
                        wait = false;
                        System.out.println("tmp 文件存在,删除它: "+tmp.getAbsolutePath()+",耗时:"+ waitTime+"ms");
                        Thread.sleep(400);
                        tmp.delete();
                    }else {
                        if(waitTime > 3000){
                            wait = false;
                            System.out.println("tmp 文件一直没有,3000ms超时 ");
                        }
                    }
                }

                //exec.getInputStream().re


                //<dependency>
                //  <groupId>com.github.mjeanroy</groupId>
                //  <artifactId>exiftool-lib</artifactId>
                //  <version>2.6.0</version>
                //</dependency>

                //https://stackoverflow.com/questions/3021420/java-api-to-exiftool


                //https://exiftool.org/forum/index.php?topic=7909.0
                //exiftool -Subject=Keyword \
                //-Title=Title \
                //-ImageDescription=Description image.png
                //1 没有旋转 2- 90 3 -180
                //Horizo​​ntal(normal)
                //I prefer using numbers
                //
                //-orientation#=6
                //
                //From exiftool documentation
                //
                //1 = Horizontal (normal)
                //2 = Mirror horizontal
                //3 = Rotate 180
                //4 = Mirror vertical
                //5 = Mirror horizontal and rotate 270 CW
                //6 = Rotate 90 CW
                //7 = Mirror horizontal and rotate 90 CW
                //8 = Rotate 270 CW
               // String oritationCmd =  exifToolPath + " -Orientation=1 -n "+ compressedFile.getAbsolutePath();
               //  Runtime.getRuntime().exec(oritationCmd);

            }

            System.out.println("单张图片压缩: 耗时:"+(System.currentTimeMillis() - start)/1000f+"s, 文件大小:"
                    +formatFileSize(file.length())+"-->"+formatFileSize(compressedFile.length())+",压缩掉了 "+(file.length()-compressedFile.length())*100/file.length()+"%");
            if(file.getName().endsWith(".jpg")){
                boolean renameTo = compressedFile.renameTo(file);
                if(renameTo){
                    return BaseBean.successBack();
                }
            }
            FileUtil.copy(compressedFile,file,true);
            compressedFile.delete();
            return BaseBean.successBack();

        } catch (Throwable e) {
            e.printStackTrace();
            return BaseBean.error("exception",e.getMessage());
        }
    }

    private static String getToolPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("mac") &&os.indexOf("os")>0){
            return "exiftool";
        }
        String path0 = ImgCompressor.class.getResource("/").getPath();
        if(path0.endsWith("/")){
            return path0+"exiftool.exe";
        }
        return path0+"/exiftool.exe";
    }
}
