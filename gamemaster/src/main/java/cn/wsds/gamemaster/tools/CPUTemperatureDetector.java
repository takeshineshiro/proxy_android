package cn.wsds.gamemaster.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * CPU 温度探测
 */
public class CPUTemperatureDetector {

	private static final float INVALID_VALUE = -100f;
	private static final String ROOT_DIR = "/sys/class/thermal";
	private static final String PREFIX = "thermal_zone";

	private File file;

	public CPUTemperatureDetector() {
		file = findFile();
	}

	/**
	 * 取当前CPU温度。
	 * 
	 * @return 当前CPU温度（摄氏），或当检测失败时返回负值
	 */
	public float get() {
		if (file == null) {
			return INVALID_VALUE;
		}
		FileInputStream input = null;
		try {
			input = new FileInputStream(file);
			byte[] buf = new byte[128];
			int bytes = input.read(buf);
			if (bytes <= 0) {
				return INVALID_VALUE;
			}
			int value = bytesToInt(buf, bytes);
			if (value <= 0) {
				return INVALID_VALUE;
			}
			return (value < 200) ? value : value * 0.001f;
		} catch (IOException e) {
			file = null;
			return INVALID_VALUE;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {}
			}
		}
	}
	
	private static File findFile() {
		File root_dir = new File(ROOT_DIR);
		if (!root_dir.exists() || !root_dir.isDirectory()) {
			return null;
		}
		// 在指定目录里枚举所有路径
		File[] files = root_dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return filename.startsWith(PREFIX);
			}
		});
		if (files == null || files.length == 0) {
			return null;
		}
		// 遍历所有目录，如果其中的type文件以关键字"cpu"结尾，则
		// 使用该目录里的temp文件
		byte[] buf = new byte[128];
		for (File dir : files) {
			if (!dir.isDirectory()) {
				continue;
			}
			File file = new File(dir, "type");
			int bytes = readFromFile(file, buf);
			if (isSuffixCPU(buf, bytes)) {
				file = new File(dir, "temp");
				if (file.exists() && file.isFile()) {
					return file;
				}
			}
		}
		// 没有找到type谁的里的关键字，使用第一个目录里的temp文件
		for (File dir : files) {
			if (dir.isDirectory()) {
				File file = new File(dir, "temp");
				if (file.exists() && file.isFile()) {
					return file;
				}
			}
		}
		//
		return null;
	}

	/**
	 * 判断给定内存区里的内容，是否以关键字cpu结尾
	 * 
	 * @param buf
	 *            内存区
	 * @param size
	 *            内存区里的有效字节数
	 * @return true表示是以关键字cpu结尾
	 */
	private static boolean isSuffixCPU(byte[] buf, int size) {
		if (size < 3) {
			return false;
		}
		for (int i = size - 1; i >= 0; --i) {
			byte b = buf[i];
			if (b <= 0x20) {
				// 跳到空白字符
				continue;
			}
			if (i < 2) {
				return false;
			}
			if (b != 'u' && b != 'U') {
				return false;
			}
			--i;
			b = buf[i];
			if (b != 'p' && b != 'P') {
				return false;
			}
			--i;
			b = buf[i];
			return b == (byte) 'c' || b == (byte) 'C';
		}
		return false;
	}

	/**
	 * 从指定文件里读
	 * 
	 * @return 读取的字节数。或当读取失败时返回-1
	 */
	private static int readFromFile(File file, byte[] buf) {
		FileInputStream input = null;
		try {
			input = new FileInputStream(file);
			return input.read(buf);
		} catch (IOException e) {
			return -1;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {}
			}
		}

	}

	/**
	 * 将指定内存区里的，表示数值的文字转换成整数（类似于atoi）
	 */
	private static int bytesToInt(byte[] buf, int size) {
		int value = 0;
		for (int i = 0; i < size; ++i) {
			byte b = buf[i];
			if (b < 0x30 || b > 0x39) {
				break;
			}
			value *= 10;
			value += b - 0x30;
		}
		return value;
	}
}
