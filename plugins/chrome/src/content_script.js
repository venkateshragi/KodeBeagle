var pathname = window.location.pathname.substr(1);
//fileMetadata is metadata received from Kodebeagle server
var fileMetadata = {};
//fileMetadata is parsed and converted to lineMetadata that is a map from lineNumber to all types information
var lineMetadata = {};

if(pathname.endsWith(".java")) {
    document.getElementsByClassName("file")[0].addEventListener('click', getMatchedTypes, false);
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
      if (xhr.readyState == 4 && xhr.status == 200) {
        fileMetadata = JSON.parse(xhr.responseText).hits.hits[0];
        if(sessionStorage.methodName) {
            fileMetadata._source.methodDefinitionList.some(function(methodDef) {
                var sessionStoredArgs = JSON.parse(sessionStorage.args);
                if(methodDef.method == sessionStorage.methodName && methodDef.argTypes.length == sessionStoredArgs.length) {
                    var matchFound = true;
                    for(var i = 0; i < sessionStoredArgs.length; i++) {
                        if(methodDef.argTypes[i] != sessionStoredArgs[i])
                            matchFound = false;
                    }
                    if(matchFound) {
                        var methodInfo = methodDef.loc.split("#");
                        $('html,body').animate({
                            scrollTop: $("td[data-line-number="+ methodInfo[0] +"]").offset().top
                        }, 2000);
                        return true;
                    }
                }
            })
            sessionStorage.removeItem("methodName");
            sessionStorage.removeItem("args")
        }
        parseFileMetadata(fileMetadata);
//        console.log("response : " + xhr.responseText);
      }
    };
    xhr.open("POST", "http://192.168.2.67:9200/filemetadata/typefilemetadata/_search", true);
    xhr.setRequestHeader("Content-type", "application/json");
    xhr.send("{\"query\":{\"bool\":{\"should\":[{\"term\":{\"typefilemetadata.fileName\":\""+ pathname + "\"}}]}}}");
}

function parseFileMetadata(fileMetadata) {
    /*if(!fileMetadata)
        return;*/
    fileMetadata._source.typeLocationList.forEach(function(typeLocation){
        var lineInfo = typeLocation.loc.split("#");
        if(!lineMetadata[lineInfo[0]])
            lineMetadata[lineInfo[0]] = [];
        typeLocation.type = "typeLocation";
        lineMetadata[lineInfo[0]].push(typeLocation);
    });
    fileMetadata._source.methodTypeLocation.forEach(function(methodType){
        var typeInfo = methodType.loc.split("#");
        if(!lineMetadata[typeInfo[0]])
            lineMetadata[typeInfo[0]] = [];
        methodType.type = "methodType";
        lineMetadata[typeInfo[0]].push(methodType);
    });
    fileMetadata._source.internalRefList.forEach(function(internalRef){
        var refInfo = internalRef.childLine.split("#");
        if(!lineMetadata[refInfo[0]])
            lineMetadata[refInfo[0]] = [];
        internalRef.type = "internalRef";
        lineMetadata[refInfo[0]].push(internalRef);
    });
}

function getMatchedTypes(event) {
    var elements = document.getElementsByClassName("selection_box");
    while (elements[0]) {
        elements[0].parentNode.removeChild(elements[0]);
    }
    if(event.ctrlKey) {
        var lineNumber = event.target.closest("tr").children[0].attributes["data-line-number"].value;
//        console.log("line number:" + lineNumber);
        var lineMeta = lineMetadata[lineNumber];
        if(!lineMeta)
            return;
        var sourceFile = "";
        //TODO this some needs to be replaced once exact column can be extracted from click event
        lineMeta.some(function(typeInfo){
            if(typeInfo.type == "typeLocation") {
                var typeId = typeInfo.id;
                fileMetadata._source.externalRefList.some(function(externalRef){
                    if(externalRef.id === typeId) {
                        sourceFile = externalRef.fqt;
                        return true;
                    }
                });
                return true;
            }
            if(typeInfo.type == "methodType") {
                var typeId = typeInfo.id;
                fileMetadata._source.externalRefList.some(function(externalRef){
                    if(externalRef.id === typeId) {
                        sourceFile = externalRef.fqt;
                        sessionStorage.methodName = typeInfo.method;
                        sessionStorage.args = JSON.stringify(typeInfo.argTypes);
                        return true;
                    }
                });
                return true;
            }
            if(typeInfo.type == "internalRef") {
                var parentLineInfo = typeInfo.parentLine.split("#");
                $('html,body').animate({
                    scrollTop: $("td[data-line-number="+ parentLineInfo[0] +"]").offset().top
                }, 2000);
                return true;
            }
        });
        /*var typeId = "";
        fileMetadata._source.typeLocationList.some(function(typeInformation){
            var lineInfo = typeInformation.loc.split("#");
            if(lineNumber === lineInfo[0]) {
                typeId = typeInformation.id;
                return true;
            }
        });
        var sourceFile = "";
        fileMetadata._source.externalRefList.some(function(externalRef){
            if(externalRef.id === typeId) {
                sourceFile = externalRef.fqt;
                return true;
            }
        });*/
        if(sourceFile) {
            sourceFile = sourceFile.replace(/\./g, "/");
            getMatchedSourceFiles(sourceFile, {left:event.pageX, top:event.pageY});
        }
    }
}

var matchedSourceFiles = [];
function getMatchedSourceFiles(sourceFile, position) {
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
      if (xhr.readyState == 4 && xhr.status == 200) {
        matchedSourceFiles = JSON.parse(xhr.responseText).hits.hits;
//        console.log("response : " + xhr.responseText);
        showDropDown(matchedSourceFiles, position);
      }
    };
    xhr.open("POST", "http://192.168.2.67:9200/sourcefile/typesourcefile/_search", true);
    xhr.setRequestHeader("Content-type", "application/json");
    xhr.send("{\"query\":{\"bool\":{\"should\":[{\"wildcard\":{\"fileName\":\"*"+ sourceFile + "*\"}}]}}}");
}

function showDropDown(matchedSourceFiles, position) {
    var div = createSelectionDiv(matchedSourceFiles);
    document.body.insertBefore(div, document.body.firstChild);
    $(div).offset(position);
    /*if(matchedSourceFiles.length == 1)
        window.location.href = "https://github.com/" + matchedSourceFiles[0]._source.fileName;*/

    /*var selection  = chrome.extension.getURL ("selection_box.html");
    var options = "";
    matchedSourceFiles.forEach(function(sourceFile) {
        var option = "<option value=\"" + sourceFile + "\">" + sourceFile + "</option>"
        options = options + option;
    });
    var dropDownElement = document.getElementsByClassName("file")[0];
    dropDownElement.style.display = "block";
    var popupWidth  = dropDownElement.offsetWidth;
    var popupHeight = dropDownElement.offsetHeight;

    if(mouseX+popupWidth > windowWidth)
        popupLeft = mouseX-popupWidth;
    else
        popupLeft = mouseX;

    if(mouseY+popupHeight > windowHeight)
        popupTop = mouseY-popupHeight;
    else
        popupTop = mouseY;
    if(popupLeft < 0)
      popupLeft = 0;
    if(popupTop < 0)
      popupTop = 0;

    $('div').offset({top:popupTop,left:popupLeft});*/
}

function createSelectionDiv(matchedSourceFiles) {
    var div = document.createElement ("div");
    div.className = "selection_box";
    div.style.zIndex = 1000;
    div.style.width = "500px";
    div.style.background = "inherit";
    div.style.borderStyle = "ridge";
    matchedSourceFiles.forEach(function(sourceFile) {
        var span = document.createElement("span");
        span.className = "source_file";
        span.textContent = sourceFile._source.fileName;
        span.onclick = function() {
            window.location.href = "https://github.com/" + sourceFile._source.fileName;
        }
        div.appendChild(span);
    });
    return div;
}