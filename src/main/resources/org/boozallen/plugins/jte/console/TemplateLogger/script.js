document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll("span[first-line='true'").forEach( (element) => {
        element.textContent = element.textContent.replace(/(\r\n|\n|\r)/gm,"")
        element.innerHTML += " <span class='jte-collapsible' isHidden='false' onclick='jteCollapse(this)'>(hide)</span><br>"
    });
}, false);

function jteCollapse(link){
    var span = link.parentNode;
    var isHidden = link.getAttribute("isHidden"); 
    var display; 
    if (isHidden == 'true'){
        link.textContent = "(show)";
        link.style.visibility = "visible"; 
        link.setAttribute("isHidden", "false")
        display = "none";
    }else{
        link.textContent = "(hide)" 
        link.setAttribute("isHidden","true")
        link.style.visibility = "";
        display = "inline"; 
    }
    var id = span.getAttribute("jte-id");
    
    document.querySelectorAll(`span[jte-id='${id}'][first-line='false']`).forEach( (element) => {
        element.style.display = display;
    });
}