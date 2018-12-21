# Minimal makefile for Sphinx documentation
#

# You can set these variables from the command line.
SPHINXOPTS    =
SPHINXBUILD   = sphinx-build
SPHINXPROJ    = JenkinsTemplatingEngine
SOURCEDIR     = .
BUILDDIR      = _build
DOCSDIR       = docs

# Put it first so that "make" without argument is like "make help".
help:
	@$(SPHINXBUILD) -M help "$(SOURCEDIR)" "$(BUILDDIR)" $(SPHINXOPTS) $(O)

.PHONY: help Makefile docs build 

# cleanup
clean: 
	rm -rf $(DOCSDIR)/$(BUILDDIR) build bin 

# build image 
image: 
	docker build $(DOCSDIR) -t sdp-docs

# build docs 
docs: 
	make clean 
	make image
	docker run -v $(shell pwd)/$(DOCSDIR):/app sdp-docs $(SPHINXBUILD) -M html "$(SOURCEDIR)" "$(BUILDDIR)" $(SPHINXOPTS) $(O)

# hot reload
livedocs:
	make image
	cd $(DOCSDIR)
	docker run -p 8000:8000 -v $(shell pwd)/$(DOCSDIR):/app sdp-docs sphinx-autobuild -b html $(ALLSPHINXOPTS) . $(BUILDDIR)/html -H 0.0.0.0 
	cd - 

jpi:
	gradle clean jpi 

test: 
	gradle clean test 

# Catch-all target: route all unknown targets to Sphinx using the new
# "make mode" option.  $(O) is meant as a shortcut for $(SPHINXOPTS).
%: Makefile
	echo "Make command $@ not found" 