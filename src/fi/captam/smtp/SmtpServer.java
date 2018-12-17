package fi.captam.smtp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class SmtpServer {

	public static void main(String args[]) throws Exception {
		int port = 9999;
		ServerSocket ssock = new ServerSocket(port);
		print("smtp server listening on port: "+port);
		while (true) {
			Socket sock = ssock.accept();
			String ip = sock.getInetAddress().getHostAddress();
			print("new connection from: "+ip);
			Session s = new Session(sock);
			if (!s.handle()) break;
		}
		print("exit");
	}
	
	public static class Session {
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
		public void writeLine(String msg) throws Exception { bos.write((msg+"\r\n").getBytes()); flush(); print("S:"+msg);}
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
			print("C:"+sb.toString());
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
			String cmd = readLine(); add(cmd);
			if (cmd.startsWith("HELO")||cmd.startsWith("EHLO")) {
				writeLine("250 foo.bar.com ready");
				while (true) {
					cmd = readLine(); add(cmd);
					if (cmd.equals("QUIT")) { break;}
					if (cmd.equals("DATA")) {
						writeLine("354 End data with <CR><LF>.<CR><LF>");
						String msg = readData();
						writeLine("250 Ok: mail queued.");
						add(msg);
						continue;
					}
					if (cmd.startsWith("MAIL FROM:")) {
						writeLine("250 Ok");
						continue; 
					}
					if (cmd.startsWith("RCPT TO:")) { 
						writeLine("250 Ok");
						continue; 
					}
					print("UNKNOWN COMMAND:"+cmd); break;
				}
			} 
			writeLine("221 Bye");
			s.close();
			print("YOU GOT MAIL:\n================================");
			print(mail.toString());
			print("================================");
			return true;
		}
	}
	
	public static void print(String msg) {System.out.println(msg);}

	
}
