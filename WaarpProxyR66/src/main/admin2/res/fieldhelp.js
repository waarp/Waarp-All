/* start multiple load function - allows scripts to load/register gracefully - design by Simon Willison */
function addLoadEvent(func) {
  var oldonload = window.onload;
  if (typeof window.onload != 'function') {
    window.onload = func;
  } else {
    window.onload = function() {
      if (oldonload) {
        oldonload();
      }
      func();
    }
  }
}
addLoadEvent(init);
addLoadEvent(function() { 
/* more code to run on page load */ 
addLoadEvent(openClose);
});
/* end multiple load function */	


/* start function init() - design by Gez Lemon */
function init()
{
	var objImage = document.getElementsByTagName('img');
	var objHelp, objAnchor, objClone;

	for (var iCounter=0; iCounter<objImage.length; iCounter++)
	{
		if (objImage[iCounter].className == 'help')
		{
			objHelp = document.getElementById('container' + objImage[iCounter].id);
			objAnchor = document.createElement('a');
			objClone = objImage[iCounter].cloneNode(true);

			objAnchor.setAttribute('href', '#');
			objAnchor.onclick = function() {return openClose(this);};
			objAnchor.appendChild(objClone);
			objHelp.style.display = 'none';

			objImage[iCounter].parentNode.replaceChild(objAnchor, objImage[iCounter]);
		}
	}
}
/* end function init() */


/* start function openClose(objElement) - design by Gez Lemon */
function openClose(objElement)
{
	var objElement = objElement;
	var objHelp = document.getElementById('container' + objElement.firstChild.id);

	if (objHelp)
	{
		if (objHelp.style.display == 'none')
		{
			objHelp.style.display = 'block';
		}
		else
		{
			objHelp.style.display = 'none';
		}
	}

	return false;
}
/* end function openClose(objElement) */








