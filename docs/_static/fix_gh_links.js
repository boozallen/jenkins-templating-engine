elements = document.querySelector("span.rst-current-version").innerText.split(" ");
version = elements[ elements.length - 2 ];
edit_on_github = document.querySelector("a.fa.fa-github");
edit_on_github.href = edit_on_github.href.replace("<REPLACE>", `${version}/docs/`);