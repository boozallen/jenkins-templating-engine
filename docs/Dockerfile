FROM squidfunk/mkdocs-material:7.3.3
RUN pip install \
    mkdocs-gen-files \
    mkdocs-exclude \
    mike \
    pymdown-extensions && \
    git config --global user.name "docs deployer" && \
    git config --global user.email "null@null.com" && \
    git config --global credential.helper 'store --file=/root/.git-credentials'