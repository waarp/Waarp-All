var chunkSize = 1024*1024; // bytes
var timeout = 10; // millisec
var idHiddenHash = "finalhash"; // default id of hidden input field

function setIdHiddenHash(id) {
  idHiddenHash = id;
}

function uuidv4() {
  return ([1e7]+-1e3+-4e3+-8e3+-1e11).replace(/[018]/g, c =>
    (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
  );
}

function uuidlong() {
  var array = new Uint32Array(1);
  crypto.getRandomValues(array);
  return array[0];
}

// preprocessFile(file)
function initSha256(file) {
  handleFiles(file.file);
  file.preprocessFinished();
}

function handlePromiseFiles(file) {
  return new Promise((resolve, reject) => {
    handleFiles(file);
    var repeatCheck = function() {
       var hash = document.getElementById(idHiddenHash).value;
       if (hash == "") {
          setTimeout(function() {repeatCheck()}, 500);
       } else {
          resolve(hash);
       }
     };
    setTimeout(function() {repeatCheck()}, 500);
  });
}

function handleFiles(file) {
    document.getElementById(idHiddenHash).value = "";
    if(file===undefined){
        return;
    }
    var SHA256 = CryptoJS.algo.SHA256.create();
    var counter = 0;
    var self = this;

    loading(file,
        function (data) {
            var wordBuffer = CryptoJS.lib.WordArray.create(data);
            SHA256.update(wordBuffer);
            counter += data.byteLength;
        }, function (data) {
            var hash = SHA256.finalize().toString();
            document.getElementById(idHiddenHash).value = hash;
            console.log("END COMPUTE: " + hash);
        });
};

function clear(){
    lastOffset = 0;
    chunkReorder = 0;
    chunkTotal = 0;
}

function loading(file, callbackProgress, callbackFinal) {
    var offset     = 0;
    var size=chunkSize;
    var partial;
    var index = 0;

    if(file.size===0){
        callbackFinal();
    }
    while (offset < file.size) {
        partial = file.slice(offset, offset+size);
        var reader = new FileReader;
        reader.size = chunkSize;
        reader.offset = offset;
        reader.index = index;
        reader.onload = function(evt) {
            callbackRead(this, file, evt, callbackProgress, callbackFinal);
        };
        reader.readAsArrayBuffer(partial);
        offset += chunkSize;
        index += 1;
    }
}

function callbackRead(obj, file, evt, callbackProgress, callbackFinal){
    if( true ){
        callbackRead_buffered(obj, file, evt, callbackProgress, callbackFinal);
    } else {
        callbackRead_waiting(obj, file, evt, callbackProgress, callbackFinal);
    }
}

var lastOffset = 0;
var chunkReorder = 0;
var chunkTotal = 0;
// time reordering
function callbackRead_waiting(reader, file, evt, callbackProgress, callbackFinal){
    if(lastOffset === reader.offset) {
        //console.log("[",reader.size,"]",reader.offset,'->', reader.offset+reader.size,"");
        lastOffset = reader.offset+reader.size;
        callbackProgress(evt.target.result);
        if ( reader.offset + reader.size >= file.size ){
            lastOffset = 0;
            callbackFinal();
        }
        chunkTotal++;
    } else {
        //console.log("[",reader.size,"]",reader.offset,'->', reader.offset+reader.size,"wait");
        setTimeout(function () {
            callbackRead_waiting(reader,file,evt, callbackProgress, callbackFinal);
        }, timeout);
        chunkReorder++;
    }
}
// memory reordering
var previous = [];
function callbackRead_buffered(reader, file, evt, callbackProgress, callbackFinal){
    chunkTotal++;

    if(lastOffset !== reader.offset){
        // out of order
        //console.log("[",reader.size,"]",reader.offset,'->', reader.offset+reader.size,">>buffer");
        previous.push({ offset: reader.offset, size: reader.size, result: reader.result});
        chunkReorder++;
        return;
    }

    function parseResult(offset, size, result) {
        lastOffset = offset + size;
        callbackProgress(result);
        if (offset + size >= file.size) {
            lastOffset = 0;
            callbackFinal();
        }
    }

    // in order
    //console.log("[",reader.size,"]",reader.offset,'->', reader.offset+reader.size,"");
    parseResult(reader.offset, reader.size, reader.result);

    // resolve previous buffered
    var buffered = [{}]
    while (buffered.length > 0) {
        buffered = previous.filter(function (item) {
            return item.offset === lastOffset;
        });
        buffered.forEach(function (item) {
            //console.log("[", item.size, "]", item.offset, '->', item.offset + item.size, "<<buffer");
            parseResult(item.offset, item.size, item.result);
            previous.remove(item);
        })
    }

}

Array.prototype.remove = Array.prototype.remove || function(val){
    var i = this.length;
    while(i--){
        if (this[i] === val){
            this.splice(i,1);
        }
    }
};
