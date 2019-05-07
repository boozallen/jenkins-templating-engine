document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll("span[first-line='true'").forEach( (element) => {
        element.textContent = element.textContent.replace(/(\r\n|\n|\r)/gm,"")
        element.innerHTML += " <span class='jte-collapsible' isHidden='false' onclick='jteCollapse(this)'>(hide)</span><br>"
    });
}, false);

function jteCollapse(link){
    console.log("inside jteCollapse()")
    var span = link.parentNode;
    var isHidden = link.getAttribute("isHidden"); 
    var display; 
    console.log("isHidden = " + isHidden);
    // if (isHidden === 'true'){
    //     console.log("text was hidden");
    //     link.textContent = "(show)";
    //     link.style.visibility = "visible"; 
    //     link.setAttribute("isHidden", 'false');
    //     display = "none";
    // }else{
    //     console.log("text was visible");
    //     link.textContent = "(hide)";
    //     link.setAttribute("isHidden",'true');
    //     link.style.visibility = "";
    //     display = "inline"; 
    // }

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
    console.log("setting display to " + display);
    document.querySelectorAll(`span[jte-id='${id}'][first-line='false']`).forEach( (element) => {
        element.style.display = display;
    });
}