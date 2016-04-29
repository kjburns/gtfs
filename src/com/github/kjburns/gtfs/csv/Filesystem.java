package com.github.kjburns.gtfs.csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public class Filesystem {
	public static String readAllText(File f) throws IOException {
		String ret;
		try (FileInputStream fis = new FileInputStream(f)) {
			ret = Filesystem.readAllText(fis);
		} catch (Exception e) {
			throw e;
		}
		
		return ret;
	}
	
	public static String readAllText(InputStream is) throws IOException {
		StringBuilder sb = new StringBuilder(512);
		try (Reader r = new InputStreamReader(is, "UTF-8")) {
			int c = 0;
			while (c != -1) {
				c = r.read();
				sb.append((char) c);
			}
			r.close();
		} catch (IOException e) {
			throw e;
		}
		return sb.toString();
	}

	public static void writeAllText(File f, String data) {
		try (Writer w = new OutputStreamWriter(
				new FileOutputStream(f), "UTF-8")){
			w.write(data);
			w.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
