package com.example.demo.controller;

import com.example.demo.utils.*;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
public class hello extends BaseController {

    @Value("${File.change.count}")
    private int changeCount;

    @PostMapping("/uploadAPI")
    public PageData he1(HttpServletRequest request, HttpServletResponse response) {
        String TIME = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String reffer = request.getHeader("referer");
        response.setHeader("Access-Control-Allow-Origin", "*"); //跨域  服务器 处理。
        String logPath = "D:/log/Log_" + TIME + ".txt";
        PageData pd = this.getPageData();
        String mac = pd.get("MMM")!=null ? pd.getString("MMM"):"A000000";
        String enMd5 = MD5.md5( mac+"Server").substring(5,22);
        int count = 0; //记录用户当天第几次操作上传动作。
        String DDD = pd.get("DDD")!=null ? pd.getString("DDD"):"123456";
        //传 MMM：mac   DDD：mac 通过 mac+"server".截取5-22.
        if (reffer == null ||
                reffer.equalsIgnoreCase("no-referrer-when-downgrade") ||
                reffer.indexOf("login_stock") == -1 ||
                pd.containsKey(" ") ||
                pd.containsValue(" ")
            ||!enMd5.equals(DDD)//如果不判断 店铺编号  可以去掉这句判断
                ) {
            pd.put("falg", false);
            pd.put("msg", "无效请求");
            return pd;
        }
        long start = 0;
        try {
            if (isMultipartContent(request)) {
                MultipartHttpServletRequest MultipartRequest = (MultipartHttpServletRequest) request;
                Map<String, MultipartFile> mapFiles = MultipartRequest.getFileMap();
                String dirPath = "D:/" + Const.FILEPATHFILE + TIME + "/";
                String extName = "";
                if (mapFiles.size() == 0) {
                    pd.put("msg", "请选择您要上传的文件");
                    pd.put("falg", false);
                    return pd;
                }

                for (Map.Entry<String, MultipartFile> fileMap : mapFiles.entrySet()) {
                    String file = fileMap.getValue().getOriginalFilename();

                    if (file.lastIndexOf(".") >= 0) {
                        extName = file.substring(file.lastIndexOf(".")); //文件后缀
                    }
                    Pattern pattern = Pattern.compile("/*png|/*jpg|/*jpeg|/*mp4");
                    Matcher matcher = pattern.matcher(extName.toLowerCase());
                    if (!matcher.find()) {
                        pd.put("msg", "请检查上传文件的格式");
                        pd.put("falg", false);
                        return pd;
                    }
                }
                //===========  正则取log日志  ======================
                FileUpload.mkdirsmy("D:/log","Log_" + TIME + ".txt" );//根据日期创建文件夹
                Pattern pattern = Pattern.compile("/*"+mac+"/*");
                Pattern methodParrern = Pattern.compile("/*uploadAPI/*");
                /*start =  System.currentTimeMillis();
                BufferedReader fileReader = new BufferedReader(new FileReader(new File(logPath)));
                List<String> fileRead = new ArrayList<String>();
                fileReader.lines()
                        .filter(o -> Pattern.compile("/*uploadAPI/*").matcher(o).find())
                        .filter(o1 -> Pattern.compile("/*"+mac+"/*").matcher(o1).find())
                        .sorted((o1, o2) -> o2.substring(o2.length()-1).compareTo(o1.substring(o1.length()-1)))
                        .collect(Collectors.toList())
                        .forEach(o->{
                            if(o!=null){
                                fileRead.add(o.substring(o.length()-1));
                            }
                        });
                System.out.println("+++++++++++++++++++++++++++++Lambda:"+(System.currentTimeMillis()-start)+"ms. ++++++++++++++++++++++++++++++++++");*/
                start =  System.currentTimeMillis();
                String read = null;
                BufferedReader fileReader1 = new BufferedReader(new FileReader(new File(logPath)));//根据时间以及路径下的 txt去读取
                List<String> fileRead1 = new ArrayList<String>();//创建一个list接收
                while ((read = fileReader1.readLine())!=null){//赋值给string read 如果不为空的话
                    Matcher matcher = pattern.matcher(read);//验证规则 验证规范
                    if(mac.equals("A000000")){continue;}
                    Matcher methodMatcher = methodParrern.matcher(read);//相当于被测试的内容  验证upload 是否存在
                    if(methodMatcher.find() && matcher.find()){//如果两者都为true
                        fileRead1.add(read.substring(read.length()-1));//那么 将read 的值 进行截取最后一位
                    }
                }
                fileRead1.sort( ((o1, o2) -> o2.compareTo(o1)));//排序
                System.out.println("+++++++++++++++++++++++++++++Normal:"+(System.currentTimeMillis()-start)+"ms. ++++++++++++++++++++++++++++++++++");
               // fileReader.close();
                fileReader1.close();
                if(fileRead1.size() > 0){
                    count = Integer.valueOf(fileRead1.get(0));//获取值
                }

                if(count >= changeCount){
                    pd.put("msg", "您当天超过申请次数限制，请明天再试");
                    pd.put("falg", false);
                    return pd;
                }
                //===========  正则取log日志  ======================
                count += 1;

                for (Map.Entry<String, MultipartFile> fileMap : mapFiles.entrySet()) {
                    String file = fileMap.getValue().getOriginalFilename();
                    if (fileMap.getValue().getSize() == 0) {
                        continue;
                    }
                    if (file.lastIndexOf(".") >= 0) {
                        extName = file.substring(file.lastIndexOf(".")); //文件后缀
                    }

                    String newFileName = this.get32UUID() + extName;
                    FileUpload.mkdirsmy(dirPath, newFileName);//根据日期创建文件夹
                    //=========================  日志记录  =============================
                    String Mac = DateUtils.sdfDate()+":info [/uploadAPI] " +
                            "CustomerNo:"+mac+" --- "+ dirPath + newFileName+" ::"+ count +"\r\n";
                    FileCopyUtils.copy(Mac.getBytes(),
                            new BufferedOutputStream(new FileOutputStream(new File(logPath),true)));
                    //=========================  日志  =================================
                    System.out.println(dirPath + newFileName);
                    if (extName.toLowerCase().endsWith("png") ||
                            extName.toLowerCase().endsWith("jpg") ||
                            extName.toLowerCase().endsWith("jpeg")
                            ) {
                        Thumbnails.of(fileMap.getValue().getInputStream())//图片尺寸不变，压缩图片文件大小
                                .scale(1f)//缩放 参数1为最高品质
                                .outputQuality(0.5f)
                                .toFile(dirPath + newFileName);//这就是操作
                        pd.put(fileMap.getKey(), Const.FILEPATHFILE + TIME + "/" + newFileName);
                        continue;
                    } else {
                        int resultInt = FileCopyUtils.copy(new BufferedInputStream(fileMap.getValue().getInputStream()),
                                new BufferedOutputStream(new FileOutputStream(new File(dirPath + newFileName))));
                        pd.put(fileMap.getKey(), Const.FILEPATHFILE + TIME + "/" + newFileName);
                    }
                }

                pd.put("msg", "上传成功");
                pd.put("falg", true);
                return pd;

            } else {
                pd.put("falg", false);
                pd.put("msg", "请检查文件格式");
                return pd;
            }
        } catch (Exception e) {
            e.printStackTrace();
            pd.put("falg", false);
            pd.put("msg", "异常，文件上传失败");
            return pd;
        }
    }


    @PostMapping("/uploadApiDelete")
    public Map<?, ?> he11(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*"); //跨域  服务器 处理。
        PageData pd = this.getPageData();
        String mac = pd.get("MMM")!=null ? pd.getString("MMM"):"A000001";
        String DDD = pd.get("DDD")!=null ? pd.getString("DDD"):"123456";
        String enMd5 = MD5.md5( mac+"Server").substring(5,22);
        Map<String, Object> map = new HashMap<String, Object>();
        String TIME = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String logPath = "D:/log/Log_" + TIME + ".txt";
        String reffer = request.getHeader("referer");
        if (reffer == null ||
                reffer.equalsIgnoreCase("no-referrer-when-downgrade") ||
                reffer.indexOf("stockmanage") == -1
                ||!enMd5.equals(DDD)
                ) {
            pd.put("falg", false);
            pd.put("msg", "无效请求");
            return pd;
        }
        try {

            Map<String, String[]> pathMap = request.getParameterMap();
            String filePath = "";
            if (pathMap.size() == 0) {
                map.put("falg", false);
                map.put("msg", "参数个数无效");
                return map;
            }
            for (Map.Entry<String, String[]> fileMap : pathMap.entrySet()) {
                filePath = fileMap.getValue()[0];
                if (filePath.toLowerCase().endsWith("png") ||
                        filePath.toLowerCase().endsWith("jpg") ||
                        filePath.toLowerCase().endsWith("gif") ||
                        filePath.toLowerCase().endsWith("jpeg") ||
                        filePath.toLowerCase().endsWith("mp4") ||
                        filePath.toLowerCase().endsWith("avi") ||
                        filePath.toLowerCase().endsWith("wmv") ||
                        filePath.toLowerCase().endsWith("mpg") ||
                        filePath.toLowerCase().endsWith("rm") ||
                        filePath.toLowerCase().endsWith("rmvb") ||
                        filePath.toLowerCase().endsWith("3gp")
                        ) {
                    if (FileDelete(filePath)) {
                        map.put(fileMap.getKey() + "_falg", true);
                        map.put(fileMap.getKey(), "文件删除成功");
                        //=========================  日志记录  =============================
                        String Mac = DateUtils.sdfDate()+":info [/uploadApiDelete] " +
                                " CustomerNo:" + mac + " --- "+ filePath+" ::1\r\n";
                        FileCopyUtils.copy(Mac.getBytes(),
                                new BufferedOutputStream(new FileOutputStream(new File(logPath),true)));
                        //=========================  日志  =================================
                    } else {
                        map.put(fileMap.getKey() + "_falg", false);
                        map.put(fileMap.getKey(), "文件删除失败");
                    }
                } else {
                    map.put(fileMap.getKey() + "_falg", false);
                    map.put(fileMap.getKey(), "文件删除失败");
                }
            }
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            map.put("falg", false);
            map.put("msg", "异常，文件删除失败");
            return map;
        }
    }


    @PostMapping("/uploadExcel")
    public PageData he1Excel(MultipartFile fileExcel,HttpServletRequest request, HttpServletResponse response) {

        String TIME = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String reffer = request.getHeader("referer");
        response.setHeader("Access-Control-Allow-Origin", "*"); //跨域  服务器 处理。
        PageData pd = this.getPageData();
        String mac = pd.get("MMM")!=null ? pd.getString("MMM"):"A000000";
        String enMd5 = MD5.md5( mac+"Server").substring(5,22);
        String DDD = pd.get("DDD")!=null ? pd.getString("DDD"):"123456";
        if (reffer == null ||
                reffer.equalsIgnoreCase("no-referrer-when-downgrade") ||
                reffer.indexOf("login_stock") == -1 ||
                pd.containsKey(" ") ||
                pd.containsValue(" ")
                ||!enMd5.equals(DDD)//如果不判断 店铺编号  可以去掉这句判断
                ) {
            pd.put("falg", false);
            pd.put("msg", "无效请求");
            return pd;
        }
        long start = 0;
        try {
            if (isMultipartContent(request)) {
                MultipartHttpServletRequest MultipartRequest = (MultipartHttpServletRequest) request;
                Map<String, MultipartFile> mapFiles = MultipartRequest.getFileMap();
                String dirPath = "D:/" + Const.FILEPATHFILE + TIME + "/";
                String extName = "";
                if (mapFiles.size() == 0) {
                    pd.put("msg", "请选择您要上传的文件");
                    pd.put("falg", false);
                    return pd;
                }
                mkDir(dirPath);
                for (Map.Entry<String, MultipartFile> fileMap : mapFiles.entrySet()) {
                    String file = fileMap.getValue().getOriginalFilename();
                    System.out.println(file);
                    if (file.lastIndexOf(".") >= 0) {
                        extName = file.substring(file.lastIndexOf(".")); //文件后缀
                    }
                    Pattern pattern = Pattern.compile("/*xlsx|/*xls|/*docx|");// /*mp4
                    Matcher matcher = pattern.matcher(extName.toLowerCase());
                    if (!matcher.find()) {
                        pd.put("msg", "请检查上传文件的格式");
                        pd.put("falg", false);
                        return pd;
                    }
                    FileCopyUtils.copy(fileMap.getValue().getInputStream(),new FileOutputStream(dirPath+UUID.randomUUID()+extName));
                }

                pd.put("msg", "上传成功");
                pd.put("falg", true);
                return pd;

            } else {
                pd.put("falg", false);
                pd.put("msg", "请检查文件格式");
                return pd;
            }
        } catch (Exception e) {
            e.printStackTrace();
            pd.put("falg", false);
            pd.put("msg", "异常，文件上传失败");
            return pd;
        }
    }

    /**
     * 创建文件夹
     *
     * @param s
     * @return
     */
    private static boolean mkDir(String s) {
        File f = new File(s);
        if (!f.exists()) {
            return f.mkdirs();
        }
        return false;
    }


    /**
     * 判断是否是multipart/form-data请求
     *
     * @param request
     * @return
     */
    public static boolean isMultipartContent(HttpServletRequest request) {
        if (!"post".equals(request.getMethod().toLowerCase())) {
            return false;
        }

        String contentType = request.getContentType();  //获取Content-Type
        if ((contentType != "") && (contentType.toLowerCase().startsWith("multipart/"))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 文件删除方法。
     *
     * @param fileName
     * @return
     * @author Ma lixiang
     */
    private boolean FileDelete(String fileName) {
        boolean falg = false;
        String filePath = "D:/" + fileName;
        File fileOfDel = new File(filePath);
        if (fileOfDel.exists()) {
            falg = fileOfDel.delete();
        }
        return falg;
    }

}
