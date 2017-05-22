package cn.wsds.gamemaster.tools;


/**
 * 压缩上传工具类
 * @author Administrator
 *
 */
public class CompressUpload {	
	
	/*public void errorlogCompress(){
		// /data/data/cn.wsds.gamemaster/app_data/proxy.log
		// /data/data/cn.wsds.gamemaster/app_log/errorlog.txt
		File appData = context.getDir("data", Context.MODE_PRIVATE);
		File appLog = context.getDir("log", Context.MODE_PRIVATE);
		File proxy = new File(appData, "proxy.log");
		File errorlog = new File(appLog, "errorlog.txt");
		
		//zipFile(proxy, outputStream)
		
	}*/
	
//	public static File getProxyFile(Context context){
//		File appData = context.getDir("data", Context.MODE_PRIVATE);
//		return new File(appData, "proxy.log");
//	}
//	
//	public static void zipFile(List<File> files, ZipOutputStream outputStream){
//		int size = files.size();
//		for(int i=0; i<size; i++){
//			File file = files.get(i);
//			zipFile(file, outputStream);
//		}
//	}
//	
//	public static void zipFile(File inputFile, ZipOutputStream outputStream){
//		try {
//			if(inputFile.exists()){
//				if(inputFile.isFile()){
//					FileInputStream instream = new FileInputStream(inputFile);
//					BufferedInputStream bins = new BufferedInputStream(instream);
//					ZipEntry entry = new ZipEntry(inputFile.getName());
//					outputStream.putNextEntry(entry);
//					int len;
//					byte[] buffer = new byte[1024];
//					while((len = bins.read(buffer)) != -1){
//						outputStream.write(buffer, 0, len);
//					}
//					bins.close();
//					instream.close();
//				}else{
//                    File[] files = inputFile.listFiles();
//                    for (int i = 0; i < files.length; i++) {
//                        zipFile(files[i], outputStream);
//                    }
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
}
