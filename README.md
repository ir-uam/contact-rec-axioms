# IR axioms for contact recommendation
This repository contains the code needed to reproduce the experiments of the paper:

> J. Sanz-Cruzado, C. Macdonald, I. Ounis, P. Castells. Axiomatic Analysis of Contact Recommendation Methods in Social Networks: an IR perspective. 13th ACM Conference on Recommender Systems (ECIR 2020). Lisbon, Portugal, April 2020.

## Authors
Information Retrieval Group at Universidad Autónoma de Madrid
- Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
- Pablo Castells (pablo.castells@uam.es)

Terrier Group at University of Glasgow
- Craig Macdonald (craig.macdonald@glasgow.ac.uk)
- Iadh Ounis (iadh.ounis@glasgow.ac.uk)

## Software description
This repository contains all the needed classes to reproduce the experiments explained in the paper. The software contains the following packages:

### Algorithms
The software includes the implementation of several contact recommendation approaches which are used in our experiments.

#### Information Retrieval Models
* Probability ranking principle models
    * Binary Independent Retrieval (BIR).
    * BM25, plus versions without term discrimination and length normalization.
    * Extreme BM25 (EBM25), plus version without term discrimination.
* Language Models
    * Query Likelihood with Jelinek-Mercer smoothing (QLJM), plus versions without term discrimination and length normalization.
    * Query Likelihood with Dirichlet smoothing (QLD), plus versions without term discrimination and length normalization.
    * Query Likelihood with Laplace smoothing (QLL).
* Divergence From Randomness
    * DFRee
    * DFReeKLIM
    * DLH
    * DPH
    * PL2
* Pivoted normalization Vector Space Model (VSM)

#### Friends of friends algorithms for contact recommendation
* Most Common Neighbors
* Cosine
* Jaccard
* Adamic-Adar

Next, we include a table including the different parameter configurations we have selected for the experiments in our paper. 
We include these configurations in the `conf` folder. The notation followed is the same as the one indicated in the previous 
formulas. In addition, similarly to [1], we choose, for the directed graphs, different orientations for the target and candidate
users' neighborhoods (the incoming, outgoing or undireted neighborhood of the users). In BM25 and EBM25 we also select
a different neighborhood for the length.

| Algorithm (and variants) | Parameters |
| --- | --- |
| BM25 | <img src="https://latex.codecogs.com/gif.latex?b%20\in%20\{%200.1,0.2,0.3,...,0.8,0.9,0.999,1.0%20\}" /> <br> <img src="https://latex.codecogs.com/gif.latex?k%20\in%20\{%200.001,0.01,0.1,1,10,100,1000%20\}" /> |
| ExtremeBM25 | <img src="https://latex.codecogs.com/gif.latex?b%20\in%20\{%200.1,0.2,0.3,...,0.8,0.9,0.999,1.0%20\}" /> |
| QLD | <img src="https://latex.codecogs.com/gif.latex?\mu%20\in%20\{%200.001,0.01,0.1,1,10,100,1000%20\}" /> |
| QLJM | <img src="https://latex.codecogs.com/gif.latex?\lambda%20\in%20\{%200.1,0.2,0.3,...,0.8,0.9,0.999,1.0%20\}" /> |
| QLL | <img src="https://latex.codecogs.com/gif.latex?\gamma%20\in%20\{%200.001,0.01,0.1,1,10,100,1000%20\}" /> |
| PL2 | <img src="https://latex.codecogs.com/gif.latex?c%20\in%20\{%200.001,0.01,0.1,1,10,100,1000%20\}" /> |
| Pivoted normalization |  <img src="https://latex.codecogs.com/gif.latex?\alpha%20\in%20\{%200.1,0.2,0.3,...,0.8,0.9,0.999,1.0%20\}" /> |

### Metrics
- nDCG@k
- AUC


## System Requirements

## Installation

## Execution

## References
[1] Sanz-Cruzado, J., Castells, P.  Information Retrieval Models for Contact Recommendation in Social Networks. 
In: ECIR 2019: Advances in Information Retrieval, pp. 148–163. No. 11437 in LNCS, 
Springer International Publishing, Cologne, Germany (2019)
