function replaceAll(c,a,b){return b.replace(new RegExp(c,"g"),a)}var loadImages=function(a){var c=Date.now();var d=c;var b=function(e){if(e.length>0){var g=e.shift();g=replaceAll("__TIME_OFFSET__",(d-c),g);var f=document.createElement("img");var h=function(){d=Date.now();b(e)};if(f.attachEvent){f.attachEvent("onload",h);f.attachEvent("onerror",h)}else{f.addEventListener("load",h);f.addEventListener("error",h)}f.src=g;document.body.appendChild(f)}};b(a)};
