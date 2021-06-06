package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import db.DataBase;
import model.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.

            BufferedReader buffer = new BufferedReader(new InputStreamReader(in, "UTF-8"));

            String line = buffer.readLine();

            if(StringUtils.equals(line, null)) {
                return;
            }

            String[] token = line.split(" ");
            String url = token[1]; // request urL

            Map<String, String> params = new HashMap<>();

            int index = url.indexOf("?");

            if(StringUtils.equals(url, "/")) {
                url += "index.html";
            } else if(index != -1) {
                //전달된 정보가 있으면
                params = HttpRequestUtils.parseQueryString(url.substring(index+1));
                url = url.substring(0, index);
            }

            log.debug("[url]" + url);
            log.debug("[params]" + params);

            if(StringUtils.equals(url, "/user/create")) {
                //회원가입 정보 저장
                User userInfo = new User(params.get("userId"), params.get("password"), params.get("name"), params.get("email"));
                DataBase.addUser(userInfo);
            }

            //아 하기싫어
            //HTTP 정보 읽어오기
            while (!StringUtils.equals(line, "")) {
                line = buffer.readLine();
                log.debug("[line info]" + line);



            }

            DataOutputStream dos = new DataOutputStream(out);

            byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
           // byte[] body = "Hello World".getBytes();

            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
