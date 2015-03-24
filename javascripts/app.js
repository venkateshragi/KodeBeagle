/*global console,$,XMLHttpRequest,document,ace,Handlebars,_*/
var app = function () {

    "use strict";
    var esURL = "172.16.12.162:9201",
        resultSize = 50,
        analyzedProjContainer = $("#analyzedProj"),
        resultTreeContainer = $("#resultTreeContainer"),
        resultTreeTemplateHTML = $("#result-tree-template").html(),
        resultTemplateHTML = $("#result-template").html(),
        resultTreeTemplate = Handlebars.compile(resultTreeTemplateHTML),
        resultTemplate = Handlebars.compile(resultTemplateHTML),
        Range = ace.require('ace/range').Range,
        errorElement = $("#connectionError"),
        leftPanel = $("#leftPanel"),
        rightSideContainer = $("#rightSideContainer"),
        expandIcon = $("#expand"),
        compressIcon = $("#compress"),
        errorMsgContainer = $("#errorMsg"),
        searchMetaContainer = $('#searchMeta'),
        methodsContainer = $("#methodsContainer"),
        methodsContainerTemplateHTML = $("#common-usage-template").html(),
        methodsContainerTemplate = Handlebars.compile(methodsContainerTemplateHTML),
        fileTab = $("#fileTab"),
        methodTab = $("#methodTab");


    Handlebars.registerHelper('stringifyFunc', function (fnName, index, lines) {
        return "app." + fnName + "All('result" + index + "-editor',[" + lines + "])";
    });

    Handlebars.registerHelper('updateEditorFn', function (file) {
        return "app.showFileContent(["+JSON.stringify(file)+"])";
    });

    function init() {
        searchMetaContainer.hide();
        compressIcon.hide();
    }

    init();

    function highlightLine(editor, lineNumbers) {
        lineNumbers.forEach(function (line) {
            /*IMPORTANT NOTE: Range takes row number starting from 0*/
            var row = line - 1,
                endCol = editor.session.getLine(row).length,
                range = new Range(row, 0, row, endCol);

            editor.getSession().addMarker(range, "ace_selection", "background");
        });
    }

    function foldLines(editor, lineNumbers) {
        var nextLine = 0;
        lineNumbers.forEach(function (n) {
            if (nextLine !== n - 1) {
                var range = new Range(nextLine, 0, n - 1, 0);
                editor.getSession().addFold("...", range);
            }
            nextLine = n;
        });
        editor.getSession().addFold("...", new Range(nextLine, 0, editor.getSession().getLength(), 0));
    }

    /*function extractCode(editor, lineNumbers) {
        var relevantUsage = [];
        lineNumbers.forEach(function (n) {
            var content = editor.session.getLine(n - 1);
            relevantUsage.push(content);
        });

        resultTreeContainer.hide();
        methodTab.addClass("active");
        methodsContainer.html(methodsContainerTemplate({"examples": _.unique(relevantUsage)}));
    }*/

    function enableAceEditor(id, content, lineNumbers) {
        $("#" + id).html("");
        var editor = ace.edit(id);

        editor.setValue(content);
        editor.setReadOnly(true);
        editor.resize(true);

        editor.setTheme("ace/theme/github");
        editor.getSession().setMode("ace/mode/java", function () {

            highlightLine(editor, lineNumbers);
            foldLines(editor, lineNumbers);
        });

        editor.gotoLine(lineNumbers[lineNumbers.length - 1], 0, true);
    }

    function getFileName(filePath) {
        var elements = filePath.split("/"),
            repoName = elements[0] + "-" + elements[1],
            fileName = elements[elements.length - 1];
        return {"repo": repoName, "file": fileName};
    }

    function fetchFileQuery(fileName) {
        return {"query": {"term": {"typesourcefile.fileName": fileName}}}
    }

    function queryES(indexName, queryBody, resultSize, successCallback) {
        $.es.Client({
            host: esURL,
            log: 'trace'
        }).search({
            index: indexName,
            size: resultSize,
            body: queryBody
        }).then(function (result) {
                successCallback(result.hits.hits);
            }, function (err) {
                errorMsgContainer.text(err.message);
                errorElement.slideDown("slow");
                errorElement.slideUp(2500);
            }
        )
    }

    function updateLeftPanel(processedData) {
        var projects = [],
            groupedByRepos = _.groupBy(processedData, function (entry) {
                return entry.repo;
            });

        projects = _.map(groupedByRepos, function (files, label) {
            return {
                name: label,
                files: _.unique(files, _.iteratee('name'))
            }
        });

        fileTab.addClass("active");
        resultTreeContainer.html(resultTreeTemplate({"projects": projects}));
    }

    function updateRightSide(processedData) {
        var files = processedData.slice(0, 1);

        files.forEach(function (fileInfo, index) {

            queryES("sourcefile", fetchFileQuery(fileInfo.path), 1, function (result) {
                var id = "result" + index,
                    content = "";
                if (result.length > 0) {
                    content = result[0]._source.fileContent;
                    enableAceEditor(id + "-editor", content, fileInfo.lines);
                } else {
                    $("#" + id).hide();
                }
            });
        });

        $("#results").html(resultTemplate({"files": files}));
    }

    function filterRelevantTokens(searchString, tokens) {
        var result = searchString.split(",").map(function (term) {

            var matchingTokens = [],
                correctedTerm = term.trim().replace(/\*/g, ".*").replace(/\?/g, ".{1}");

            matchingTokens = tokens.filter(function (tk) {
                return (tk["importName"]).search(correctedTerm) >= 0;
            });

            return matchingTokens;
        });

        return _.flatten(result);
    }

    function buildSearchString(str) {
        var result = "";
        if (str[0] === "\'") {
            result = str.substr(1, str.length - 2);
        } else {
            result = str.split(",").map(function (entry) {
                return "*" + entry.trim();
            }).join(",");
        }
        return result;
    }

    function processResult(searchString, data) {
        var result = [],
            intermediateResult = [],
            groupedData = [], matchingImports = [];

        groupedData = _.groupBy(data, function (entry) {
            return entry._source.file;
        });

        intermediateResult = _.map(groupedData, function (files, fileName) {
            var labels = getFileName(fileName),
                lineNumbers = [];

            files.forEach(function (f) {
                var matchingTokens = filterRelevantTokens(searchString, f._source.tokens),
                    possibleLines = _.pluck(matchingTokens, "lineNumbers");

                matchingImports = matchingImports.concat(matchingTokens.map(function (x) {
                    return x.importName;
                }));

                lineNumbers = lineNumbers.concat(possibleLines);
            });

            lineNumbers = (_.unique(_.flatten(lineNumbers))).sort(function (a, b) {
                return a - b;
            });

            return {
                path: fileName,
                repo: labels.repo,
                name: labels.file,
                lines: lineNumbers,
                score: files[0]._source.score
            };

        });

        /* sort by descending usage/occurrence with weighted score */
        result = _.sortBy(intermediateResult, function (elem) {
            var sortScore = (elem.score * 10000) + elem.lines.length;
            return -sortScore;
        });

        console.log(_.unique(matchingImports));
        return result;
    }

    function updateView(searchString, data) {
        var processedData = processResult(searchString, data);

        analyzedProjContainer.hide();
        searchMetaContainer.show();

        updateLeftPanel(processedData);
        updateRightSide(processedData);
    }

    function getQuery(queryString) {
        var terms = queryString.split(","),
            mustTerms = terms.map(function (queryTerm) {
                var prefix = (queryTerm.search(/\*/) >= 0 || queryTerm.search(/\?/) >= 0) ? "wildcard" : "term";
                var result = {};
                result[prefix] = {"custom.tokens.importName": queryTerm.trim()};
                return result;
            });

        return {
            "bool": {
                "must": mustTerms,
                "must_not": [],
                "should": []
            }
        };
    }

    function search(queryString) {
        var correctedQuery = buildSearchString(queryString),
            queryBlock = getQuery(correctedQuery);

        queryES("betterdocs", {
            "query": queryBlock,
            "sort": [
                {"score": {"order": "desc"}}]
        }, resultSize, function (result) {
            updateView(correctedQuery, result);
        });
    }

    function updateConfig(url, size) {
        esURL = url.trim();
        resultSize = size;
    }

    function expandResultView() {
        leftPanel.hide();
        expandIcon.hide();
        rightSideContainer.addClass("fullWidth");
        compressIcon.show();
    }

    function compressResultView() {
        leftPanel.show();
        compressIcon.hide();
        rightSideContainer.removeClass("fullWidth");
        expandIcon.show();
    }

    function collapseUnnecessaryLines(id, lineNumbers) {
        var editor = ace.edit(id);
        foldLines(editor, lineNumbers);
    }

    function expandAllBlocks(id, lineNumbers) {
        var editor = ace.edit(id);
        editor.getSession().foldAll();
        lineNumbers.forEach(function (n) {
            editor.getSession().unfold(n);
        });
    }

    function showRelevantFiles() {
        methodsContainer.hide();
        methodTab.removeClass("active");
        fileTab.addClass("active");
        resultTreeContainer.show();
    }

    function showFreqUsedMethods() {
        resultTreeContainer.hide();
        fileTab.removeClass("active");
        methodTab.addClass("active");
        methodsContainer.show();
    }

    return {
        search: search,
        saveConfig: updateConfig,
        expand: expandResultView,
        compress: compressResultView,
        collapseAll: collapseUnnecessaryLines,
        expandAll: expandAllBlocks,
        showFiles: showRelevantFiles,
        showMethods: showFreqUsedMethods,
        showFileContent: updateRightSide
    };
}();