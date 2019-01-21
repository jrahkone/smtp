/*
Copyright (c) 2018, Jukka Rahkonen, Captam Finland Ltd
All rights reserved.

FreeBSD License

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The views and conclusions contained in the software and documentation are those
of the authors and should not be interpreted as representing official policies,
either expressed or implied, of the Simple SMTP Server project.
 */
package fi.captam.smtp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple SMTP Server for testing purposes.
 * Checkout: git clone https://github.com/jrahkone/smtp
 * @author jukka.rahkonen@iki.fi
 */
public class SmtpServer extends Thread {

	public static void main(String args[]) throws Exception {
		SmtpServer smtp = new SmtpServer(2525);
		print("started");
		//Thread.sleep(10000);
		//print("kill");
		//smtp.kill();
		//print("exit");
	}

	int port;
	List<String> mails = new ArrayList<String>();
	SmtpServer(int port) { this.port=port;this.start();}
	
	@Override
	public void run() {
		try {
			ServerSocket ssock = new ServerSocket(port);
			print("smtp server listening on port: "+port);
			while (true) {
				Socket sock = ssock.accept();
				String ip = sock.getInetAddress().getHostAddress();
				print("new connection from: "+ip);
				Session s = new Session(sock);
				if (!s.handle()) break;
			}
			print("Server done");
		} catch (Exception e) { print("exception:"+e);}	
	}

	public void kill() {
		try {
			Socket s = new Socket("localhost",port);
			OutputStream os = s.getOutputStream();
			os.write("KILL\r\n".getBytes());
			os.flush();
		} catch(Exception e) {print("could not kill server:"+e);}
	}

	public class Session {
		Socket s;
		BufferedInputStream bis;
		BufferedOutputStream bos;
		StringBuilder mail = new StringBuilder();
		private void add(String line) {mail.append(line+"\n");}
		public Session(Socket s) throws Exception {
			this.s = s;
			bos = new BufferedOutputStream(s.getOutputStream());
			bis = new BufferedInputStream(s.getInputStream());
		}
		public void flush() throws Exception { bos.flush();}
		public void writeLine(String msg) throws Exception { bos.write((msg+"\r\n").getBytes()); flush();}
		public String readLine() throws Exception {
			StringBuilder sb = new StringBuilder();
			int last=0;
			while (true) {
				int b = bis.read();
				if (b < 0) break;
				if (b=='\n'||last=='\r') { break; }
				last = b;
				if (b=='\r') continue;
				sb.append((char)b);
			}
			return sb.toString();
		}
		public String readData() throws Exception {
			StringBuilder msg = new StringBuilder();
			while (true) {
				String line = readLine();
				msg.append(line+"\n");
				if (line.equals(".")) break;
			}
			return msg.toString();
		}
		public boolean handle() throws Exception {
			writeLine("220 foo.bar.com simple smtp server");
			String cmd = readLine();
			if (cmd.startsWith("HELO")||cmd.startsWith("EHLO")) {
				writeLine("250 foo.bar.com ready");
				while (true) {
					cmd = readLine();
					if (cmd.equals("QUIT")) { break;}
					if (cmd.equals("DATA")) {
						writeLine("354 End data with <CR><LF>.<CR><LF>");
						add(cmd);
						String msg = readData();
						writeLine("250 Ok: mail queued.");
						add(msg);
						continue;
					}
					if (cmd.startsWith("MAIL FROM:")) {
						add(cmd);
						writeLine("250 Ok");
						continue; 
					}
					if (cmd.startsWith("RCPT TO:")) {
						add(cmd);
						writeLine("250 Ok");
						continue; 
					}
					print("UNKNOWN COMMAND:"+cmd); break;
				}
			} 
			writeLine("221 Bye");
			s.close();
			if (cmd.equals("KILL")) { return false;}
			mails.add(mail.toString());
			print("GOT MAIL: "+mail.length()+" bytes");
			writeToFile("mail.html");
			return true;
		}
	}

	public void clear() { mails = new ArrayList<String>();}
	public boolean anyMailContains(String str) {
		for (String mail:mails) if(mail.contains(str)) return true;
		return false;
	}
	public String getLastMail() {if (mails.size()==0) return null; return mails.get(mails.size()-1);}
	public boolean lastMailContains(String str) {if(getLastMail()==null) return false; return getLastMail().contains(str);}
	public void writeToFile(String fname) throws Exception { writeFile(fname,getLastMail());}
	public static void print(String msg) {System.out.println(msg);}
	public static void writeFile(String fname, String data) throws Exception {
		if (data==null) return;
	    BufferedWriter writer = new BufferedWriter(new FileWriter(fname));
	    writer.write(data); writer.close();
	}
}
