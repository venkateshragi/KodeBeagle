'use strict';

var PATH_NAME = window.location.pathname.substr(1);
var KODEBEAGLE_SERVER = "http://192.168.2.67:9200";//192.168.2.67:9200
//fileMetadata is metadata received from Kodebeagle server
var linesMeta = {};
//fileMetadata is parsed and converted to lineMetadata that is a map from lineNumber to all types information
var filesMeta = {};
var lineMetaInfo = {},fileMetaInfo = {},storage,loader;

function navigateToSelection(fileMetadata){
    if(storage.getValue('methodName')){
        fileMetadata._source.methodDefinitionList.some(function(methodDef) {
            var sessionStoredArgs = storage.getValue('args');
            if(methodDef.method == storage.getValue('methodName') && checkArgsEquality(methodDef.argTypes,sessionStoredArgs)){
                var lineInfo = methodDef.loc.split("#");
                scrollAndSelect(lineInfo);
            }
        });
        storage.deleteValue("methodName");
        storage.deleteValue("args");
    }
}

function checkArgsEquality(methodDefArgs,sessionStoredArgs){
    if (methodDefArgs.length !== sessionStoredArgs.length) {
        return false;
    }else{
        for(var i = 0; i < sessionStoredArgs.length; i++) {
            if(methodDefArgs[i] !== sessionStoredArgs[i])
                return false;
        }
        return true;
    }
}

function columnCount(rawRowText){
    if(!rawRowText){
        return 0;
    }

    var colCount = 0;
    if(rawRowText.search('\t') > -1){
        colCount++;
        while(rawRowText.search('\t') > -1){
            rawRowText = rawRowText.replace('\t','')
            colCount++;
        }
    }else{
        var arrTexts = rawRowText.split(' ');
        for(var eachIndex=0;eachIndex<arrTexts.length;eachIndex++){
            if(arrTexts[eachIndex]){
                break;
            }
            colCount++;
        };  
    }
    return colCount;
}


function addColumnNumbers(){
    $('.blob-code-inner').each(function(rowIndex){
        var rawRowText = $(this).text(), colCount = columnCount(rawRowText);
       
        if($(this).find('span').length ===0){
            var doc = document.createDocumentFragment(),
                spaces = columnCount(rawRowText);
            if(spaces > 0){
                var emptySpan = document.createElement('span');
                emptySpan.innerHTML = rawRowText.substr(0,spaces);
                doc.appendChild(emptySpan);
            }
            var arrStr = splitString(rawRowText.substr(spaces,rawRowText.length));
            
            this.textContent = "";
            arrStr.forEach(function(eachValue){
                var spaces = columnCount(eachValue);
                var span = document.createElement('span');
                span.innerHTML = eachValue;
                spaces = eachValue.trim().length>0 ?spaces:0;
                span.setAttribute('data-column-number',colCount+spaces);
                colCount += eachValue.length;
                doc.appendChild(span);
            })
            $(this).append(doc); 
        }else{
            $(this).find('span').each(function(index){
                if(index === 0 && this.previousSibling && this.previousSibling.nodeType ===3 && this.previousSibling.nodeValue.trim()){
                    var doc = document.createDocumentFragment(), 
                        span = document.createElement('span'),
                        text = this.previousSibling.nodeValue, spaces = columnCount(text);
                    this.previousSibling.nodeValue = "";  
                    
                    if(spaces > 0){
                        var emptySpan = document.createElement('span');
                        emptySpan.innerHTML = text.substr(0,spaces);
                        doc.appendChild(emptySpan);
                    }
                    span.innerHTML = text.substr(spaces,text.length);
                    span.setAttribute('data-column-number',colCount);
                    doc.appendChild(span);
                    $(doc).insertBefore(this);
                    colCount += (match($(this).text()))? text.trim().length : text.trim().length+1; 
                }
            
                $(this).attr('data-column-number',colCount);
                colCount += $(this).text().length; 
                if(this.nextSibling && this.nextSibling.nodeType === 3){
                    var text = this.nextSibling.nodeValue; 

                    /*if(match(text)){*/
                        var arrStr = splitString(text);
                        
                        this.nextSibling.nodeValue = "";
                        var doc = document.createDocumentFragment();
                        
                        arrStr.forEach(function(eachValue){
                            var spaces = columnCount(eachValue);
                            /*if(spaces > 0){
                                var emptySpan = document.createElement('span');
                                emptySpan.innerHTML = eachValue.substr(0,spaces);
                                doc.appendChild(emptySpan);
                            }*/

                            var span = document.createElement('span');
                            span.innerHTML = eachValue;
                            spaces = eachValue.trim().length>0 ?spaces:0;
                            span.setAttribute('data-column-number',colCount+spaces);
                            colCount += eachValue.length;
                            doc.appendChild(span);
                        })
                        $(doc).insertAfter(this);           
                    /*}
                    else{
                        var span = document.createElement('span');
                        span.innerHtml = text;
                        var spaces = columnCount(text);
                        span.setAttribute('data-column-number',colCount+spaces);
                        colCount += text.length; 
                        $(this.nextSibling).wrap(span);               
                    }*/
                }            
            });  
        }  
    });
}

function splitString(str){
    var arrStr = [],
        index = 0;

    function checkSpecialChar(str){
        var specialChars = "()[],;";
        for(var i = 0; i < str.length;i++){
            if(specialChars.indexOf(str[i]) > -1){
                return str[i];
            }
        }
        return false;
    }
    
    while(str.split(checkSpecialChar(str)).length > 1){
        var arr = str.split(checkSpecialChar(str));  
        if(arr[0]){
            arrStr[index] = arr[0];
            arrStr[index+1] = checkSpecialChar(str);
            index+=2;        
        }else{
            arrStr[index] = checkSpecialChar(str);
            index++;
        }
        arr.splice(0,1);
        str = arr.join(checkSpecialChar(str));
    }
    if(str.length > 0){
        arrStr[index] = str;
    }
    return arrStr;
}

function match(str){
    return (str.search('\\.') > -1 || str.search('\\(') > -1 || str.search('\\)') > -1 || str.search('\\[') > -1 );
}

function parseFileMetadata(hits) {
    linesMeta = {},filesMeta = {};
    hits.forEach(function(eachHit){
        var lineMetadata = {};
        function getParentRef(methodType){
            var methodDefList = eachHit._source.methodDefinitionList;
            for(var index = 0;index<methodDefList.length;index++){
                var methodDef = methodDefList[index],parentRef;
                if(methodDef.method == methodType.method && checkArgsEquality(methodDef.argTypes,methodType.argTypes)){
                    parentRef = methodDef.loc;
                    break;
                }
            };
            return parentRef;
        };
        eachHit._source.typeLocationList.forEach(function(typeLocation){
            var lineInfo = typeLocation.loc.split("#");
            if(!lineMetadata[lineInfo[0]])
                lineMetadata[lineInfo[0]] = [];
            typeLocation.type = "typeLocation";
            lineMetadata[lineInfo[0]].push(typeLocation);
        });
        eachHit._source.methodTypeLocation.forEach(function(methodType){
            var typeInfo = methodType.loc.split("#");
            if(!lineMetadata[typeInfo[0]])
                lineMetadata[typeInfo[0]] = [];
            if(methodType.id === -1 && getParentRef(methodType)){
                methodType.type = "internalRef";
                methodType.childLine = methodType.loc;
                methodType.parentLine = getParentRef(methodType);
                lineMetadata[typeInfo[0]].push(methodType);
            }else{
                methodType.type = "methodType";
                lineMetadata[typeInfo[0]].push(methodType);
            }
        });
        eachHit._source.internalRefList.forEach(function(internalRef){
            var refInfo = internalRef.childLine.split("#");
            if(!lineMetadata[refInfo[0]])
                lineMetadata[refInfo[0]] = [];
            internalRef.type = "internalRef";
            lineMetadata[refInfo[0]].push(internalRef);
        });
        linesMeta[eachHit._source.fileName] = lineMetadata;
        filesMeta[eachHit._source.fileName] = eachHit;
    }); 
    storage.setValue('linesMeta',linesMeta);
    storage.setValue('filesMeta',filesMeta);
}

function highliteReferences(lineMetadata){
    for(var eachLine in lineMetadata){
        lineMetadata[eachLine].forEach(function(eachColumn){
            if(eachColumn.type === 'internalRef'){
                var parentLine = eachColumn.parentLine.split('#');
                var childLine = eachColumn.childLine.split('#');
                //if(parentLine[0] != childLine[0]){
                    createLinks(childLine);
                //}
            }else{
                createLinks(eachColumn.loc.split('#'));
            }
        });
    }
}

function createLinksForImports(){    
    $('.blob-code-inner').each(function(rowIndex){
        if($(this).find('.pl-k').text() === 'import')
            $(this).find('.pl-smi').addClass('referenced-links');
    });
}

function createLinks(lineInfo){
    var element = $("td[data-line-number="+ lineInfo[0] +"]+td").find("span[data-column-number="+lineInfo[1]+"]")[0];
    $(element).addClass('referenced-links');
}

function getChildLines(lineMeta){
    var references = [];
    for(var eachLine in lineMetaInfo){
        lineMetaInfo[eachLine].forEach(function(eachObj){
            if(eachObj.type === 'internalRef'){
                var childLine = eachObj.childLine.split('#');
                var child = eachObj.parentLine.split('#');
                if(lineMeta[0] === child[0] && lineMeta[1] === child[1] && childLine[0] != child[0]){
                    references.push(eachObj.childLine);
                }
            }
        });
    };
    return references;
}

function closePopUp(){
    var elements = document.getElementsByClassName("links-box");
    while (elements[0]) {
        elements[0].parentNode.removeChild(elements[0]);
    }
}

function getMatchedTypes(event) {
    closePopUp();
    
    var target = $(event.target).hasClass('referenced-links')?event.target:$(event.target.parentNode).hasClass('referenced-links')?event.target.parentNode:null;
    if(target) {
        clearBoldLinks();
        if($($(target).siblings()[0]).text() === 'import'){
            var source = target.innerText;
            getMatchedSourceFiles(source, event);
            return;
        }

        var lineNumber = target.closest("tr").children[0].attributes["data-line-number"].value,
            lineMeta = lineMetaInfo[lineNumber], sourceFile = "";

        if(!lineMeta)
            return;
        
        lineMeta.some(function(typeInfo){
            if(typeInfo.type == "typeLocation") {
                var typeId = typeInfo.id, typeLocs = typeInfo.loc.split('#'),
                    columnValue = target.attributes['data-column-number'];

                if(columnValue && columnValue.value == typeLocs[1].trim()){
                    fileMetaInfo._source.externalRefList.some(function(externalRef){
                        if(externalRef.id === typeId) {
                            sourceFile = externalRef.fqt;
                            return true;
                        }
                    });
                    return true;
                }
            }
            if(typeInfo.type == "methodType") {
                var typeId = typeInfo.id, typeLocs = typeInfo.loc.split('#'),
                    columnValue = target.attributes['data-column-number'];

                if(columnValue && columnValue.value == typeLocs[1].trim()){ 
                    fileMetaInfo._source.externalRefList.some(function(externalRef){
                        if(externalRef.id === typeId) {
                            sourceFile = externalRef.fqt;
                            storage.setValue('methodName',typeInfo.method);
                            storage.setValue('args',typeInfo.argTypes);
                            return true;
                        }
                    });
                    return true;
                }
            }
            if(typeInfo.type == "internalRef") {
                var lineInfo = typeInfo.parentLine.split("#"),childLine = typeInfo.childLine.split("#"),
                    columnValue = target.attributes['data-column-number'].value;                

                if(columnValue == childLine[1].trim() && childLine[0] != lineInfo[0]){    
                    scrollAndSelect(lineInfo,target);
                    return true;
                }else if(columnValue == childLine[1].trim()){
                    var childLines = getChildLines(lineInfo), internalReferences = [];
                    childLines.forEach(function(eachLine){
                        var line = eachLine.split('#'),refereceObj = {}, 
                            child = $("td[data-line-number="+line[0]+"]+td"),
                            codeSnippet = child.text().trim(),
                            selectedText = target.innerHTML.trim();
                        codeSnippet = codeSnippet.replace(selectedText,"<b>"+selectedText+"</b>");
                        createBoldLink(child,selectedText);
                        refereceObj.snippet = codeSnippet;
                        refereceObj.references = eachLine;
                        internalReferences.push(refereceObj);
                    });
                    showInternalReferences(internalReferences,{left:event.pageX, top:event.pageY+10});
                    return true;
                }
            }
        });

        if(sourceFile) {
            //sourceFile = sourceFile.replace(/\./g, "/");
            sourceFile = sourceFile.replace(/\[\]/g, "");
            getMatchedSourceFiles(sourceFile, event);
        }
    }
}

function createBoldLink(child,selectedText){
    $(child).find('.referenced-links').each(function(){
        this.innerHTML = $(this).text().replace(selectedText,"<b>"+selectedText+"</b>");
    });
}

function clearBoldLinks(){
    $('.referenced-links b').each(function(){
        $(this).contents().unwrap();
    });
}

function scrollAndSelect(lineInfo,target){
    var rowEle = $("td[data-line-number="+ lineInfo[0] +"]+td"),element;
    $('html,body').animate({
        scrollTop: rowEle.offset().top - 20
    }, 500);
    selectRow(rowEle);
    if(target){
        element = rowEle.find(":contains("+target.innerText.trim()+")")[0];
        if(!element){
            element = rowEle.find("span[data-column-number="+lineInfo[1]+"]")[0];    
        }
    }else{
        element = rowEle.find("span[data-column-number="+lineInfo[1]+"]")[0];
    }
    selectText(element);
}

function selectRow(rowEle){
    $('.select-row-color').removeClass('select-row-color');
    rowEle.addClass('select-row-color');
}

function selectText(element){
    if(element){
        var selection = window.getSelection(),range = document.createRange();
        range.selectNodeContents(element);
        selection.removeAllRanges();
        selection.addRange(range);
    }
}

function getMatchedSourceFiles(sourceFile, event) {
    showLoader(event);
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
      if (xhr.readyState == 4 && xhr.status == 200) {
        var response = JSON.parse(xhr.responseText);
        hideLoader(event);
        showDropDown(response.hits.hits, {left:event.pageX, top:event.pageY+10});
        parseFileMetadata(response.hits.hits);
      }
    };
    xhr.open("POST", KODEBEAGLE_SERVER+"/filemetadata/typefilemetadata/_search", true);
    xhr.setRequestHeader("Content-type", "application/json");
    xhr.send("{\"query\":{\"bool\":{\"must\":[{\"term\":{\"fileTypes.fileType\":\""+ sourceFile + "\"}}]}}}");
}

function hideLoader(event){
    $('.reference-loader').remove();    
}

var showLoader = function(event){
    event.target.parentNode.appendChild(loader);
}

function showInternalReferences(internalReferences, position) {
    var div = createInternalRefDiv(internalReferences);
    document.body.insertBefore(div, document.body.firstChild);
    $(div).offset(position);
}

function showDropDown(matchedSourceFiles, position) {
    var div = createSelectionDiv(matchedSourceFiles);
    document.body.insertBefore(div, document.body.firstChild);
    $(div).offset(position);
}

function addCSS(){
    var style = document.createElement('style');
    style.type = 'text/css';
    style.innerHTML = '.referenced-links{cursor:pointer;} .referenced-links:hover{color:blue;text-decoration:underline;} .links-box{z-index:1;max-width:700px;max-height:315px;overflow:auto;background:inherit;border:solid 2px #337ab7;position:absolute;} .links-box .external-ref{padding:10px;cursor:pointer;display:block;border-bottom:solid 1px #ddd;} .links-box span:hover{ background:#EAEAEA;}.links-box div:hover{background : #eaeaea;} .links-box div {cursor : pointer;border-bottom : 1px solid #ddd;} .links-box div span{padding : 10px;display:table-cell;} .reference-loader{display:inline-table;}.reference-loader img{margin-bottom:-5px;} .select-row-color{background:#ddd;}';
    document.getElementsByTagName('head')[0].appendChild(style);
}

function createSelectionDiv(matchedSourceFiles) {
    var doc = document.createDocumentFragment(), div = document.createElement ("div");
    div.className = "links-box";
    if(matchedSourceFiles.length === 0){
        var span = document.createElement("span");
        span.className = 'external-ref';
        span.textContent = "Couldn't find external references";
        doc.appendChild(span);
    }else{
        matchedSourceFiles.forEach(function(sourceFile) {
            var span = document.createElement("span");
            span.className = 'external-ref';
            span.textContent = sourceFile._source.fileName;
            span.onclick = function() {
                window.location.href = "https://github.com/" + sourceFile._source.fileName;
            }
            doc.appendChild(span);
        });
    }
    div.appendChild(doc);
    return div;
}

function gotoLine(event){
    var lineInfo = event.currentTarget.attributes['data-line-info'].value;
    closePopUp();
    scrollAndSelect(lineInfo.split('#'));
}

function createInternalRefDiv(references) {
    var doc = document.createDocumentFragment(), mainDiv = document.createElement("div");
    mainDiv.className = "links-box";
    
    if(references.length === 0){
        var span = document.createElement("span");
        span.className = 'external-ref';
        span.textContent = "Couldn't find external/internal references";
        doc.appendChild(span);
    }else{
        references.forEach(function(lineInfo) {
            //var template = "<div onclick=gotoLine(event)><span>"+lineDetails[0]+":"+lineDetails[1]+"</span><span data-line-info="+lineInfo.references+">"+lineInfo.snippet+"</span></div"
            var lineDetails = lineInfo.references.split('#'),
                div = document.createElement("div");
            div.setAttribute('data-line-info',lineInfo.references);
            div.onclick = gotoLine;
            var lineSpan = document.createElement("span");
            lineSpan.style.width = '80px';
            lineSpan.textContent = lineDetails[0]+":"+lineDetails[1];
            div.appendChild(lineSpan);
            var textSpan = document.createElement("span");
            textSpan.innerHTML = lineInfo.snippet;
            div.appendChild(textSpan);
            doc.appendChild(div);
        });
    }
    mainDiv.appendChild(doc);
    return mainDiv;
}

var cloneLoader = function(){
    loader = $('.page-context-loader').clone().addClass('reference-loader')[0];
}

var Storage = (function () {
    'use strict';

    var instance;
    var parse = function(value){
        return JSON.parse(value);
    };
    var stringify = function(value){
        return JSON.stringify(value);
    };
    function init(){
        return {
            setValue:function(key,value){
                sessionStorage.setItem(key,stringify(value));
            },
            getValue:function(key){
                return parse(sessionStorage.getItem(key));
            },
            deleteValue:function(key){
                sessionStorage.removeItem(key);
            }
        }
    };
  
    return {
        getInstance:function(){
            if(!instance){
                instance = init();
            }
            return instance;
        }
    };
})();

(function(){
    'use strict';

    if(PATH_NAME.endsWith(".java")) {
        addCSS();
        createLinksForImports();
        addColumnNumbers();
        cloneLoader();
        storage = Storage.getInstance();

        document.getElementsByClassName("file")[0].addEventListener('click', getMatchedTypes, false);
        if(storage.getValue('linesMeta') && storage.getValue('linesMeta')[PATH_NAME]){
            fileMetaInfo = storage.getValue('filesMeta')[PATH_NAME];
            lineMetaInfo = storage.getValue('linesMeta')[PATH_NAME];
            navigateToSelection(fileMetaInfo);
            highliteReferences(lineMetaInfo);

            storage.deleteValue("filesMeta");
            storage.deleteValue("linesMeta");
        }else{
            var xhr = new XMLHttpRequest();
            xhr.onreadystatechange = function() {
              if (xhr.readyState == 4 && xhr.status == 200) {
                var response = JSON.parse(xhr.responseText);
                if(response.hits.hits && response.hits.hits.length > 0){
                    navigateToSelection(response.hits.hits[0]);
                    parseFileMetadata(response.hits.hits);
                    fileMetaInfo = filesMeta[PATH_NAME];
                    lineMetaInfo = linesMeta[PATH_NAME];
                    highliteReferences(lineMetaInfo)
                }
              }
            };
            xhr.open("POST", KODEBEAGLE_SERVER+"/filemetadata/typefilemetadata/_search", true);
            xhr.setRequestHeader("Content-type", "application/json");
            xhr.send("{\"query\":{\"bool\":{\"should\":[{\"term\":{\"fileName\":\""+ PATH_NAME + "\"}}]}}}");
        }
    }
})();