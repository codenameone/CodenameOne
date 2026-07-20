/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.builders;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Packages the ParparVM web application with a selected same-origin proxy. */
public final class JavaScriptProxyPackager {
    public interface Logger {
        void log(String message);
    }

    private static final Set<String> TARGETS = new HashSet<String>(Arrays.asList(
            "jakarta-servlet", "javax-servlet", "node", "php", "aws-lambda",
            "google-cloud-functions", "cloudflare-workers", "none"));

    private JavaScriptProxyPackager() {
    }

    public static File packageProxy(File appDir, File buildDir, String appName,
                                    BuildRequest request, Logger logger) throws IOException {
        boolean inject = !"false".equalsIgnoreCase(request.getArg("javascript.inject_proxy", "true"));
        String explicitTarget = trimToNull(request.getArg("javascript.proxy.target", null));
        String externalUrl = trimToNull(request.getArg("javascript.proxy.url", null));
        if (!inject) {
            logger.log("JavaScript proxy generation disabled by javascript.inject_proxy=false");
            return null;
        }
        if (externalUrl != null && explicitTarget == null) {
            injectProxyUrl(appDir, externalUrl);
            logger.log("Using external JavaScript proxy URL " + externalUrl);
            return null;
        }

        String target = explicitTarget == null ? "jakarta-servlet" : explicitTarget.toLowerCase();
        if (!TARGETS.contains(target)) {
            throw new IOException("Unsupported javascript.proxy.target '" + target
                    + "'. Expected one of " + TARGETS);
        }
        if ("none".equals(target)) {
            logger.log("JavaScript proxy bundle disabled by javascript.proxy.target=none");
            return null;
        }

        String allowedTargets = trimToNull(request.getArg("javascript.proxy.allowedTargets", null));
        if (allowedTargets == null) {
            logger.log("WARNING: The generated JavaScript proxy accepts arbitrary HTTP(S) targets. "
                    + "Set javascript.proxy.allowedTargets before deploying it publicly.");
            allowedTargets = "";
        }
        injectProxyUrl(appDir, externalUrl == null ? "/cn1-cors-proxy?_target=" : externalUrl);

        File stage = new File(buildDir, "javascript-proxy-" + target);
        deleteTree(stage);
        if (!stage.mkdirs() && !stage.isDirectory()) {
            throw new IOException("Failed to create proxy staging directory " + stage);
        }

        File output;
        if (target.endsWith("servlet")) {
            copyTree(appDir, stage);
            packageServlet(stage, target, allowedTargets);
            output = new File(buildDir, safeName(appName) + "-" + target + ".war");
        } else {
            File publicDir = "php".equals(target) ? stage : new File(stage, "public");
            copyTree(appDir, publicDir);
            packageRuntime(stage, target, allowedTargets);
            output = new File(buildDir, safeName(appName) + "-" + target + ".zip");
        }
        zip(stage, output);
        logger.log("Wrote JavaScript " + target + " deployable bundle to " + output);
        return output;
    }

    private static void packageServlet(File root, String target, String allowedTargets) throws IOException {
        String api = target.startsWith("jakarta") ? "jakarta" : "javax";
        File classes = new File(root, "WEB-INF/classes");
        classes.mkdirs();
        InputStream compiled = JavaScriptProxyPackager.class.getResourceAsStream(
                "/javascript-proxy-" + api + ".jar");
        if (compiled == null) {
            throw new IOException("Missing javascript-proxy-" + api + ".jar from the ParparVM bundle");
        }
        unzipClasses(compiled, classes);
        write(new File(root, "WEB-INF/classes/cn1-proxy-allowed-targets.txt"), allowedTargets + "\n");
        String namespace = "jakarta".equals(api) ? "https://jakarta.ee/xml/ns/jakartaee" : "http://xmlns.jcp.org/xml/ns/javaee";
        String version = "jakarta".equals(api) ? "6.0" : "4.0";
        String webXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<web-app xmlns=\"" + namespace + "\" version=\"" + version + "\">\n"
                + "  <servlet><servlet-name>cn1Proxy</servlet-name>"
                + "<servlet-class>com.codename1.corsproxy.CORSProxy</servlet-class></servlet>\n"
                + "  <servlet-mapping><servlet-name>cn1Proxy</servlet-name>"
                + "<url-pattern>/cn1-cors-proxy</url-pattern></servlet-mapping>\n"
                + "</web-app>\n";
        write(new File(root, "WEB-INF/web.xml"), webXml);
        write(new File(root, "DEPLOY.md"), "Deploy this WAR to "
                + ("jakarta".equals(api) ? "Tomcat 10.1 or newer." : "a Servlet 4 container such as Tomcat 9.")
                + "\nThe proxy endpoint is /cn1-cors-proxy?_target=<encoded-url>.\n");
    }

    private static void packageRuntime(File root, String target, String allowedTargets) throws IOException {
        String allowedJson = jsonArray(allowedTargets);
        if ("php".equals(target)) {
            write(new File(root, "cn1-cors-proxy.php"), phpProxy(allowedTargets));
            write(new File(root, ".htaccess"), "DirectoryIndex index.html\nRewriteEngine On\n"
                    + "RewriteRule ^cn1-cors-proxy$ cn1-cors-proxy.php [L,QSA]\n");
            write(new File(root, "DEPLOY.md"), "Deploy this directory on PHP 8 with mod_rewrite enabled.\n");
            return;
        }

        write(new File(root, "proxy-core.mjs"), jsProxyCore(allowedJson));
        if ("node".equals(target)) {
            write(new File(root, "server.mjs"), nodeServer());
            write(new File(root, "package.json"), "{\"type\":\"module\",\"scripts\":{\"start\":\"node server.mjs\"},"
                    + "\"engines\":{\"node\":\">=20\"}}\n");
            write(new File(root, "DEPLOY.md"), "Run npm start. PORT defaults to 8080.\n");
        } else if ("aws-lambda".equals(target)) {
            write(new File(root, "index.mjs"), lambdaHandler());
            write(new File(root, "template.yaml"), awsTemplate());
            write(new File(root, "package.json"), "{\"type\":\"module\",\"engines\":{\"node\":\">=20\"}}\n");
            write(new File(root, "DEPLOY.md"), "Deploy template.yaml with AWS SAM. Static assets are in public/.\n");
        } else if ("google-cloud-functions".equals(target)) {
            write(new File(root, "index.js"), googleHandler());
            write(new File(root, "package.json"), "{\"type\":\"module\",\"main\":\"index.js\","
                    + "\"engines\":{\"node\":\">=20\"},\"dependencies\":{\"@google-cloud/functions-framework\":\"^3.0.0\"}}\n");
            write(new File(root, "DEPLOY.md"), "Deploy the exported cn1App HTTP function; static assets are in public/.\n");
        } else if ("cloudflare-workers".equals(target)) {
            write(new File(root, "worker.mjs"), cloudflareWorker());
            write(new File(root, "wrangler.jsonc"), "{\"name\":\"cn1-javascript-app\",\"main\":\"worker.mjs\","
                    + "\"compatibility_date\":\"2026-01-01\",\"assets\":{\"directory\":\"./public\",\"binding\":\"ASSETS\"}}\n");
            write(new File(root, "DEPLOY.md"), "Deploy with wrangler deploy.\n");
        }
    }

    private static String jsProxyCore(String allowedJson) {
        return "const allowed=" + allowedJson + ";\n"
                + "function permitted(u){if(!['http:','https:'].includes(u.protocol))return false;if(!allowed.length)return true;"
                + "return allowed.some(p=>{p=p.trim();if(!p)return false;if(p.startsWith('*.'))return u.hostname.endsWith(p.slice(1));"
                + "try{return u.origin===new URL(p).origin}catch(e){return u.hostname===p}})}\n"
                + "const blocked=new Set(['host','origin','referer','connection','keep-alive','proxy-authenticate','proxy-authorization','te','trailer','transfer-encoding','upgrade']);\n"
                + "export async function proxyRequest(req,target){let url;try{url=new URL(target)}catch(e){return new Response('Invalid _target',{status:400})}"
                + "if(!permitted(url))return new Response('Target is not allowed',{status:403});const h=new Headers();"
                + "req.headers.forEach((v,k)=>{k=k.toLowerCase();if(!blocked.has(k))h.append(k==='x-cn1-cookie'?'cookie':k,v)});"
                + "const init={method:req.method,headers:h,redirect:'manual'};if(!['GET','HEAD'].includes(req.method)){init.body=req.body;init.duplex='half'}"
                + "const upstream=await fetch(url,init);const out=new Headers();upstream.headers.forEach((v,k)=>{const n=k.toLowerCase();"
                + "if(n==='set-cookie')out.append('X-CN1-Set-Cookie',v);else if(n==='location')out.append('X-CN1-Location',v);"
                + "else if(!blocked.has(n)&&n!=='content-security-policy'&&n!=='content-length')out.append(k,v)});"
                + "if((upstream.status<200||upstream.status>=300)&&upstream.status!==304){out.set('X-CN1-Status',String(upstream.status));"
                + "return new Response(upstream.body,{status:200,headers:out})}return new Response(upstream.body,{status:upstream.status,headers:out})}\n";
    }

    private static String nodeServer() {
        return "import http from 'node:http';import fs from 'node:fs';import path from 'node:path';import {Readable} from 'node:stream';import {proxyRequest} from './proxy-core.mjs';\n"
                + "const root=path.resolve('public');const types={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.mjs':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.json':'application/json','.png':'image/png','.jpg':'image/jpeg','.jpeg':'image/jpeg','.gif':'image/gif','.svg':'image/svg+xml','.ico':'image/x-icon','.wasm':'application/wasm','.res':'application/octet-stream'};\n"
                + "http.createServer(async(req,res)=>{try{const u=new URL(req.url,'http://localhost');if(u.pathname==='/cn1-cors-proxy'){"
                + "const body=['GET','HEAD'].includes(req.method)?undefined:Readable.toWeb(req);const r=new Request('http://localhost'+req.url,{method:req.method,headers:req.headers,body,duplex:'half'});"
                + "const p=await proxyRequest(r,u.searchParams.get('_target'));res.writeHead(p.status,Object.fromEntries(p.headers));if(p.body)Readable.fromWeb(p.body).pipe(res);else res.end();return;}"
                + "let f=path.resolve(root,'.'+decodeURIComponent(u.pathname));if(u.pathname==='/')f=path.join(root,'index.html');if(!(f===root||f.startsWith(root+path.sep))||!fs.existsSync(f)||fs.statSync(f).isDirectory()){res.writeHead(404);res.end();return;}"
                + "res.setHeader('content-type',types[path.extname(f)]||'application/octet-stream');fs.createReadStream(f).pipe(res)}catch(e){res.writeHead(500);res.end(String(e))}}).listen(process.env.PORT||8080);\n";
    }

    private static String lambdaHandler() {
        return "import {proxyRequest} from './proxy-core.mjs';export async function handler(e){const target=e.queryStringParameters&&e.queryStringParameters._target;"
                + "const req=new Request('https://lambda/cn1-cors-proxy',{method:e.requestContext.http.method,headers:e.headers,"
                + "body:e.body?(e.isBase64Encoded?Buffer.from(e.body,'base64'):e.body):undefined});const r=await proxyRequest(req,target);"
                + "return {statusCode:r.status,headers:Object.fromEntries(r.headers),body:Buffer.from(await r.arrayBuffer()).toString('base64'),isBase64Encoded:true}}\n";
    }

    private static String googleHandler() {
        return "import {proxyRequest} from './proxy-core.mjs';export async function cn1App(req,res){const body=['GET','HEAD'].includes(req.method)?undefined:req.rawBody;"
                + "const r=await proxyRequest(new Request('https://function/cn1-cors-proxy',{method:req.method,headers:req.headers,body}),req.query._target);"
                + "res.status(r.status);r.headers.forEach((v,k)=>res.append(k,v));res.send(Buffer.from(await r.arrayBuffer()))}\n";
    }

    private static String cloudflareWorker() {
        return "import {proxyRequest} from './proxy-core.mjs';export default {async fetch(req,env){const u=new URL(req.url);"
                + "if(u.pathname==='/cn1-cors-proxy')return proxyRequest(req,u.searchParams.get('_target'));return env.ASSETS.fetch(req)}};\n";
    }

    private static String awsTemplate() {
        return "AWSTemplateFormatVersion: '2010-09-09'\nTransform: AWS::Serverless-2016-10-31\nResources:\n"
                + "  Proxy:\n    Type: AWS::Serverless::Function\n    Properties:\n      Runtime: nodejs20.x\n"
                + "      Handler: index.handler\n      Events:\n        ProxyApi:\n          Type: HttpApi\n          Properties:\n"
                + "            Path: /cn1-cors-proxy\n            Method: ANY\n";
    }

    private static String phpProxy(String allowedTargets) {
        return "<?php\n$allowed=" + phpArray(allowedTargets) + ";$target=$_GET['_target']??'';$u=parse_url($target);"
                + "if(!$u||!in_array($u['scheme']??'',array('http','https'),true)){http_response_code(400);exit('Invalid _target');}"
                + "$ok=!count($allowed);foreach($allowed as $p){$host=$u['host']??'';if(strpos($p,'*.')===0&&str_ends_with($host,substr($p,1)))$ok=true;"
                + "else if(filter_var($p,FILTER_VALIDATE_URL)&&(($u['scheme'].'://'.$host.((isset($u['port']))?':'.$u['port']:''))===rtrim($p,'/'))) $ok=true;else if($host===$p)$ok=true;}"
                + "if(!$ok){http_response_code(403);exit('Target is not allowed');}$ch=curl_init($target);curl_setopt($ch,CURLOPT_CUSTOMREQUEST,$_SERVER['REQUEST_METHOD']);"
                + "curl_setopt($ch,CURLOPT_FOLLOWLOCATION,false);curl_setopt($ch,CURLOPT_RETURNTRANSFER,true);curl_setopt($ch,CURLOPT_HEADER,true);"
                + "$body=file_get_contents('php://input');if($body!=='')curl_setopt($ch,CURLOPT_POSTFIELDS,$body);$headers=array();foreach(getallheaders() as $k=>$v){"
                + "$n=strtolower($k);if(in_array($n,array('host','origin','referer','connection','content-length')))continue;$headers[]=($n==='x-cn1-cookie'?'Cookie':$k).': '.$v;}"
                + "curl_setopt($ch,CURLOPT_HTTPHEADER,$headers);$raw=curl_exec($ch);if($raw===false){http_response_code(502);exit(curl_error($ch));}$status=curl_getinfo($ch,CURLINFO_RESPONSE_CODE);"
                + "$hs=curl_getinfo($ch,CURLINFO_HEADER_SIZE);foreach(explode(\"\\r\\n\",substr($raw,0,$hs)) as $line){if(stripos($line,'Set-Cookie:')===0)header('X-CN1-Set-Cookie:'.substr($line,11),false);"
                + "else if(stripos($line,'Location:')===0)header('X-CN1-Location:'.substr($line,9),false);else if(strpos($line,':')!==false){$n=strtolower(trim(strstr($line,':',true)));"
                + "if(!in_array($n,array('content-length','content-security-policy','connection','transfer-encoding'),true))header($line,false);}}if(($status<200||$status>=300)&&$status!==304){header('X-CN1-Status: '.$status);http_response_code(200);}else http_response_code($status);echo substr($raw,$hs);\n";
    }

    private static void injectProxyUrl(File appDir, String url) throws IOException {
        File index = new File(appDir, "index.html");
        if (!index.isFile()) return;
        String html = new String(Files.readAllBytes(index.toPath()), StandardCharsets.UTF_8);
        String assignment = "window.cn1CORSProxyURL='" + jsEscape(url) + "';";
        String placeholder = "//INJECT-DEFAULT-PROXY";
        if (html.contains(placeholder)) {
            html = html.replace(placeholder, assignment);
        } else {
            // Add a final assignment even if a template contains a commented
            // example. Looking only for the property name can mistake that
            // example for active JavaScript and leave the proxy disabled.
            String script = "<script>" + assignment + "</script>\n";
            int head = html.toLowerCase().indexOf("</head>");
            html = head < 0 ? script + html : html.substring(0, head) + script + html.substring(head);
        }
        write(index, html);
    }

    private static String jsonArray(String csv) {
        StringBuilder out = new StringBuilder("[");
        String[] parts = csv.split("[,\\r\\n]+");
        for (String part : parts) {
            part = part.trim();
            if (part.length() == 0) continue;
            if (out.length() > 1) out.append(',');
            out.append('"').append(part.replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
        return out.append(']').toString();
    }

    private static String phpArray(String csv) {
        String json = jsonArray(csv);
        return "json_decode('" + json.replace("'", "\\'") + "',true)";
    }

    private static String jsEscape(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "").replace("\n", "");
    }

    private static String safeName(String value) {
        return value == null ? "application" : value.replaceAll("[^A-Za-z0-9._-]", "-");
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().length() == 0) return null;
        return value.trim();
    }

    private static void write(File file, String value) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8));
    }

    private static void unzipClasses(InputStream input, File output) throws IOException {
        ZipInputStream zin = new ZipInputStream(new BufferedInputStream(input));
        try {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.isDirectory() || entry.getName().startsWith("META-INF/")) continue;
                File dest = new File(output, entry.getName());
                dest.getParentFile().mkdirs();
                OutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
                try {
                    int count;
                    while ((count = zin.read(buffer)) != -1) out.write(buffer, 0, count);
                } finally {
                    out.close();
                }
            }
        } finally {
            zin.close();
        }
    }

    private static void copyTree(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            dst.mkdirs();
            File[] children = src.listFiles();
            if (children != null) for (File child : children) copyTree(child, new File(dst, child.getName()));
        } else {
            File parent = dst.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void zip(File source, File output) throws IOException {
        ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)));
        try {
            zipEntry(source, source, zout);
        } finally {
            zout.close();
        }
    }

    private static void zipEntry(File root, File current, ZipOutputStream zout) throws IOException {
        if (current.isDirectory()) {
            File[] children = current.listFiles();
            if (children != null) for (File child : children) zipEntry(root, child, zout);
            return;
        }
        String name = root.toPath().relativize(current.toPath()).toString().replace(File.separatorChar, '/');
        zout.putNextEntry(new ZipEntry(name));
        InputStream in = new BufferedInputStream(new FileInputStream(current));
        try {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) zout.write(buffer, 0, count);
        } finally {
            in.close();
            zout.closeEntry();
        }
    }

    private static void deleteTree(File file) throws IOException {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        if (!file.delete()) throw new IOException("Failed to delete " + file);
    }
}
