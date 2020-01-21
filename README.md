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

### EWC1 Experiment
The first program, EWC1, compares the nDCG values of several algorithms depending on whether the weights of the links are used to generate
the recommendation or not (i.e. if the frequencies for the IR algorithms are binary or not). This program can be used to generate
Figure 1(a).

The command to execute this program is the following:
```
java -jar contact-rec-axioms.jar ewc1 train test algorithmsFile outputDirectory directed recLength printRecs
```
where
* `train`: is the file containing the training graph.
* `test`: is the file containing the test edges.
* `algorithmsFile`: is the XML file containing the algorithms to compare
* `outputDirectory` a directory where to store the recommendations and the comparison file.
* `directed`: `true` if the graph is directed, `false` otherwise.
* `recLength`: the maximum number of recommended people for each target user (in our experiments, 10)
* `printRecs`: `true` if we want to store the recommendations, `false` if we just want the comparison file.

After execution, if the `printRecs` option is set to `true`, the output directory will contain two subdirectories: one called `weighted` which will 
contain the all the recommendations taking the weights of the links into account, and another called `unweighted` which 
will contain the recommenders when weights are not considered. The base folder will have a `ewc1.txt` file showing the comparison between
the weighted and unweighted variants. 
## References
[1] Sanz-Cruzado, J., Castells, P.  Information Retrieval Models for Contact Recommendation in Social Networks. 
In: ECIR 2019: Advances in Information Retrieval, pp. 148–163. No. 11437 in LNCS, 
Springer International Publishing, Cologne, Germany (2019)
