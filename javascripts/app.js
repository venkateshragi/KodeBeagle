/*global console,$,XMLHttpRequest,document,ace,Handlebars,_*/
var app = function () {

    "use strict";
    var esURL = "172.16.12.162:9200",
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
        compressIcon = $("#compress");

    function init() {
        errorElement.hide();
        resultTreeContainer.hide();
        compressIcon.hide();
    }

    init();

    function getMarkers(lineNumbers) {
        return lineNumbers.map(function (line) {
            /*var startMark = line - 6,
             endMark = line + 4;*/
            /*IMPORTANT NOTE: Range takes row number starting from 0*/
            return new Range(line - 1, 0, line - 1, 1000);
        });
    }

    function enableAceEditor(id, content, lineNumbers) {
        var editor = ace.edit(id),
            markers = getMarkers(lineNumbers);

        editor.resize(true);

        editor.setTheme("ace/theme/github");
        editor.getSession().setMode("ace/mode/java", function () {
            editor.getSession().foldAll();
            lineNumbers.forEach(function (n) {
                editor.getSession().unfold(n - 1);
            })
        });

        editor.setReadOnly(true);
        editor.setValue(content, 1);

        markers.forEach(function (m) {
            editor.getSession().addMarker(m, "ace_selection", "background");
        });

        editor.gotoLine(lineNumbers[lineNumbers.length - 1], 0, true);
    }

    function getFileName(filePath) {
        var elements = filePath.split("/"),
            repoName = elements[3] + "-" + elements[4],
            fileName = elements[elements.length - 1];
        return {"repo": repoName, "file": fileName};
    }

    function updateView(data) {
        var files = [], projects = [], groupedByRepos = [],
            groupedData = _.groupBy(data, function (entry) {
                return entry._source.file;
            });

        groupedByRepos = _.groupBy(data, function (entry) {
            var labels = getFileName(entry._source.file);
            return labels.repo;
        });

        projects = _.map(groupedByRepos, function (files, label) {
            var fileList = _.map(files, function (f) {
                return {name: getFileName(f._source.file).file};
            });

            return {
                name: label,
                files: _.unique(fileList, _.iteratee('name'))
            }
        });

        _.keys(groupedData).slice(0, 1).forEach(function (fileName, index) {
            var sameFile = groupedData[fileName],
                labels = getFileName(fileName),
                filePath = fileName.replace("http://github.com", "http://github-raw-cors-proxy.herokuapp.com"),
                occurences = (_.unique(_.flatten(sameFile.map(function (src) {
                    return src._source.lineNumbers;
                })))).sort();

            files.push({path: fileName, name: labels.repo + ":" + labels.file, lines: occurences});

            $.get(filePath, function (result) {
                var id = "result" + index;
                if (result !== "Not Found") {
                    enableAceEditor(id + "-editor", result, occurences);
                } else {
                    $("#" + id).hide();
                }
            });
        });

        $("#results").html(resultTemplate({"files": files}));
        analyzedProjContainer.hide();
        resultTreeContainer.show();
        resultTreeContainer.html(resultTreeTemplate({"projects": projects}));
    }

    function andQuery(terms) {
        var mustTerms = terms.map(function (queryTerm) {
            return {"term": {"custom.strings": queryTerm.trim()}};
        });

        return {
            "bool": {
                "must": mustTerms,
                "must_not": [],
                "should": []
            }
        };
    }

    function basicQuery(term) {
        return {
            "filtered": {
                "query": {
                    "query_string": {
                        "query": term
                    }
                }
            }
        };
    }

    function search(queryString) {
        var queryTerms = queryString.split(","),
            queryBlock = queryTerms.length > 1 ? andQuery(queryTerms) : basicQuery(queryTerms[0]);

        $.es.Client({
            host: esURL,
            log: 'trace'
        }).search({
            index: 'betterdocs',
            size: resultSize,
            body: {
                "query": queryBlock,
                "sort": [
                    {"score": {"order": "desc"}}]
            }
        }).then(function (resp) {
            //console.log(resp.hits.hits);
            updateView(resp.hits.hits);
        }, function (err) {
            errorElement.slideDown("slow");
            errorElement.slideUp(2500);
        });
    }

    function updateConfig(url, size) {
        esURL = url.trim();
        resultSize = size;
    }

    function expandResultView(){
        leftPanel.hide();
        expandIcon.hide();
        rightSideContainer.addClass("fullWidth");
        compressIcon.show();
    }

    function compressResultView(){
        leftPanel.show();
        compressIcon.hide();
        rightSideContainer.removeClass("fullWidth");
        expandIcon.show();
    }

    return {
        search: search,
        saveConfig: updateConfig,
        expand: expandResultView,
        compress: compressResultView
    };
}();