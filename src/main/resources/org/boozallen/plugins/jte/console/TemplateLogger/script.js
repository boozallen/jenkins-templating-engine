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
    if(isHidden === 'false'){
        link.setAttribute("isHidden",'true');
        display = 'none';
        link.textContent = "(show)";
        link.style.visibility = "visible"; 
    }else{
        link.setAttribute("isHidden",'false');
        display = 'inline';
        link.textContent = "(hide)";
        link.style.visibility = ""; 
    }
    var id = span.getAttribute("jte-id");
    document.querySelectorAll(`span[jte-id='${id}'][first-line='false']`).forEach( (element) => {
        element.style.display = display;
    });
}