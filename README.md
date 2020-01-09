# IR Axioms for contact recommendation
This repository contains the code needed to reproduce the experiments of the paper:

> J. Sanz-Cruzado, C. Macdonald, I. Ounis, P. Castells. Axiomatic Analysis of Contact Recommendation Methods in Social Networks: an IR perspective. 13th ACM Conference on Recommender Systems (ECIR 2020). Lisbon, Portugal, April 2020.

## Authors
Information Retrieval Group at Universidad Autónoma de Madrid
- Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
- Pablo Castells (pablo.castells@uam.es)

Terrier Group at University of Glasgow
- Craig Macdonald
- Iadh Ounis

## Software description
This repository contains all the needed classes to reproduce the experiments explained in the paper. The software contains the following packages:

### Algorithms
The software includes the implementation of several contact recommendation approaches which are used in our experiments.

#### Information Retrieval Models for contact recommendation

#### Friends of friends algorithms for contact recommendation

Next, we include a table including the different parameter configurations we have selected for the experiments in our paper. We include these configurations in the `conf` folder. The notation followed is the same as the one indicated in the previous formulas.

| Algorithm | Parameters |
| --- | --- |
| BM25 | b \in {0.1,0.2,...,0.8,0.9,0.999,1.0} <br> k \in {0.001,0.01,0.1,1,10,100,1000} |
| ExtremeBM25 | b \in {0.1,0.2,...,0.8,0.9,0.999,1.0} |
| QLD | \mu \in {0.001,0.01,0.1,1,10,100,1000} |
| QLJM | \lambda \in {0.1,0.2,...,0.8,0.9,0.999,1.0} |
| QLL | \gamma \in {0.001,0.01,0.1,1,10,100,1000} |
| PL2 | c \in {0.001,0.01,0.1,1,10,100,1000} |

### Metrics
- nDCG@k
- AUC


## System Requirements

## Installation

## Execution

