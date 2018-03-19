var fs = require('fs');
var net = require('net');

const http = require('http');

const hostname = '127.0.0.1';
const port = 9988;

/*
It looks terrible.
Need to rewritten to avoid code duplication
*/

const clientF = new net.Socket();
const clientB = new net.Socket();

var lastAliveB = Date.now() //-1
var lastAliveF = Date.now()

clientF.on('error', () => {
});
clientB.on('error', () => {
});


function myFunc(arg) {
  clientB.connect(9997, 'localhost', () => {
      console.log('connected to backend!\n');
      lastAliveB = Date.now()
      clientB.end();
  });

  clientF.connect(8888, 'localhost', () => {
      console.log('connected to frontend!\n');
      lastAliveF = Date.now()
      clientF.end();
  });


  setTimeout(myFunc, 5000, arg + 1);
}

setTimeout(myFunc, 1000, 0);

const server = http.createServer((req, res) => {
  res.statusCode = 200;
  res.setHeader('Content-Type', 'text/plain');

  var statusF = '';
  var now = Date.now();
  if (lastAliveF < 0) {
    statusF = 'DOWNi';
  } else if (now - lastAliveF > 10000) {
    statusF = 'DOWN about ' + Math.floor((now - lastAliveF ) / 1000) + ' seconds';
  } else {
    statusF = 'UP';
  }

  if (lastAliveB < 0) {
    statusB = 'DOWN';
  } else if (now - lastAliveB > 10000) {
    statusB = 'DOWN about ' + Math.floor((now - lastAliveB ) / 1000) + ' seconds';
  } else {
    statusB = 'UP';
  }

  res.end('Frontend\n' +
           'Last:    ' + fs.readFileSync('/home/knnet/bundles/frontend/LAST') +
           'Running: ' + fs.readFileSync('/home/knnet/bundles/frontend/RUN') +
           'Status: '  + statusF + '\n' +
           'Backend\n' +
           'Last:    ' + fs.readFileSync('/home/knnet/bundles/backend/LAST') +
           'Running: ' + fs.readFileSync('/home/knnet/bundles/backend/RUN') +
           'Status: '  + statusB + '\n' +
           ''
  );
});

server.listen(port, hostname, () => {
  console.log(`Server running at http://${hostname}:${port}/`);
});


