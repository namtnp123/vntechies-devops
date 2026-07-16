FROM jenkins/jnlp-agent-docker

user root
# Install necessary packages
RUN apk update 

RUN apk add --no-cache \
        openssh-server \
        bash \
        curl \
        git \
        maven \
        gradle \
        nodejs \
        npm \
        python3 \
        py3-pip

RUN apk add nodejs nodejs-current npm

# RUN apk add --no-cache helm 

# RUN  curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
