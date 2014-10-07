var fs = require('fs');
var http = require('http');
var webRoot = require('path').resolve(__dirname)
var writeFile = require('jsi/test/server-file').writeFile;
var os = require('os');



var ips = getServerIP();
writeConfig();
setInterval(writeConfig,1000*60);

http.createServer(function (req, res) {
	console.log('request:'+req.url);
	setTimeout(function(){
		writeFile(webRoot,req,res)
	}, Math.random() * 1000 * 3);
}).listen(8080);
console.log('server started on port :8080\n ips:\n',ips)





/**
 * 讲测试服务器信息输出到工程的assets 目录
 */
function writeConfig(){
	//console.info('write back test-server.json')
	fs.writeFile(__dirname+'/../assets/test-server.json',
		JSON.stringify({
			home:'http://'+ips[0]+':8080/',
			createTime:+new Date()
		}))
}
/**
 * 获取指定网卡的IP
 * @param name 网卡名
 * @param family IP版本 IPv4 or IPv6
 * @returns ip
 */
function getServerIP(name,family) {
	family = family || 'IPv4';
    //所有的网卡
    var ifaces = os.networkInterfaces();
    //返回IP地址
    var ips = [];
    for (var dev in ifaces) {
    	var face = ifaces[dev];
    	//console.log(face)
        for(var i = 0; i<face.length;i++){
        	var item = face[i];
        	if (!item.internal && item.family == family && (name == null || name == dev)) {
            	ips.push(item.address);
        	}
        }
    }
    return ips;
}
