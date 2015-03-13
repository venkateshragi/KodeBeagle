/*global console,$,XMLHttpRequest,document,ace,Handlebars,_*/
var app = function () {

    "use strict";
    var esURL = "172.16.12.162:9200",
        resultSize = 50,
        source = $("#result-template").html(),
        template = Handlebars.compile(source),
        Range = ace.require('ace/range').Range;


    function getMarkers(lineNumbers) {
        return lineNumbers.map(function (line) {
            var startMark = line - 6,
                endMark = line + 4;
            return new Range(startMark, 0, endMark, 0);
        });
    }

    function enableAceEditor(id, content, lineNumbers) {
        var editor = ace.edit(id),
            markers = getMarkers(lineNumbers);

        editor.resize(true);

        editor.setTheme("ace/theme/github");
        editor.getSession().setMode("ace/mode/java");
        editor.setReadOnly(true);
        editor.setValue(content, 1);

        markers.forEach(function (m) {
            editor.getSession().addMarker(m, "ace_active-line", "background");
        });
        editor.gotoLine(lineNumbers[lineNumbers.length - 1], 0, true);
    }

    function getFileName(filePath) {
        var elements = filePath.split("/"),
            repoName = elements[3] + "-" + elements[4],
            fileName = elements[elements.length - 1];
        return repoName + ":" + fileName;
    }

    function updateView(data) {
        var files = [],
            groupedData = _.groupBy(data, function (entry) {
                return entry._source.file
            });

        _.keys(groupedData).slice(0, 1).forEach(function (fileName, index) {
            var sameFile = groupedData[fileName],
                filePath = fileName.replace("http://github.com", "http://github-raw-cors-proxy.herokuapp.com"),
                occurences = (_.unique(_.flatten(sameFile.map(function (src) {
                    return src._source.lineNumbers;
                })))).sort();

            files.push({path: fileName, name: getFileName(fileName), lines: occurences});

            $.get(filePath, function (result) {
                var id = "result" + index;
                if (result !== "Not Found") {
                    enableAceEditor(id + "-editor", result, occurences);
                } else {
                    $("#" + id).hide();
                }
            });
        });

        $("#results").html(template({"files": files}));
    }

    function search(queryString) {
        var mustTerms = queryString.split(",").map(function (queryTerm) {
            return {"term": {"custom.strings": queryTerm.trim()}};
        });

        $.es.Client({
            host: esURL,
            log: 'trace'
        }).search({
            index: 'betterdocs',
            size: resultSize,
            body: {
                "query": {
                    "bool": {
                        "must": mustTerms,
                        "must_not": [],
                        "should": []
                    }
                },
                "sort": [
                    {"score": {"order": "desc"}}]
            }
        }).then(function (resp) {
            //console.log(resp.hits.hits);
            updateView(resp.hits.hits);
        }, function (err) {
            console.log(err.message);
        });
    }

    function updateConfig(url, size) {
        esURL = url.trim();
        resultSize = size;
    }

    function displayRepoList(containerId) {
        $.get("../resources/repos.json", function (result) {
            _.sortBy(result, 'stargazersCount').forEach(function (proj) {
                var label = proj.login + "/" + proj.name;
                $("#" + containerId).append($('<option>', {
                    value: label,
                    text: label
                }));
            });
        });
    }

    displayRepoList("repoList");

    return {
        search: search,
        saveConfig: updateConfig
    };
}();