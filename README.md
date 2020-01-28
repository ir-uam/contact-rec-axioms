# IR axioms for contact recommendation
This repository contains the code needed to reproduce the experiments of the paper:

> J. Sanz-Cruzado, C. Macdonald, I. Ounis, P. Castells. [Axiomatic Analysis of Contact Recommendation Methods in Social Networks: an IR perspective](http://ir.ii.uam.es/pubs/ecir2020.pdf). 13th ACM Conference on Recommender Systems (ECIR 2020). Lisbon, Portugal, April 2020.

## Authors
Information Retrieval Group at Universidad Autónoma de Madrid
- Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
- Pablo Castells (pablo.castells@uam.es)

Terrier Group at University of Glasgow
- Craig Macdonald (craig.macdonald@glasgow.ac.uk)
- Iadh Ounis (iadh.ounis@glasgow.ac.uk)

## Software description
This repository contains all the needed classes to reproduce the experiments explained in the paper. The software contains the following packages:

- `es.uam.eps.ir.contactrecaxioms.data`: Classes for handling the ratings by users for items. Extension of the RankSys preference data that use graphs.
- `es.uam.eps.ir.contactrecaxioms.graph`: Classes for handling network data.
- `es.uam.eps.ir.contactrecaxioms.main`: Main programs and auxiliar classes.
- `es.uam.eps.ir.contactrecaxioms.metrics`: Classes implementing the metrics used in the experiments which are not provided by RankSys.
- `es.uam.eps.ir.contactrecaxioms.recommenders`: Implementation of recommendation algorithms.
- `es.uam.eps.ir.contactrecaxioms.utils`: Additional classes, useful for the rest of the program.

## System Requirements
**Java JDK:** 1.8 or above.

**Maven:** tested with version 3.6.0.

## Installation
In order to install this program, you need to have Maven (https://maven.apache.org) installed on your system. Then, download the files into a directory, and execute the following command:
```
mvn compile assembly::single
```
If you do not want to use Maven, it is still possible to compile the code using any Java compiler. In that case, you will need the following libraries:
- Ranksys version 0.4.3: http://ranksys.org
- Colt version 1.2.0: https://dst.lbl.gov/ACSSoftware/colt
- Google MTJ version 1.0.4: https://github.com/fommil/matrix-toolkits-java
- Terrier version 5.1: http://terrier.org/
- FastUtil version 8.3.0: http://fastutil.di.unimi.it/

## Execution
The descriptions for the different programs is included in the Wiki for this project. We include here the links to the descriptions of each program. 

* [EWC1](https://github.com/ir-uam/contact-rec-axioms/wiki/EWC1)
* [EWC2](https://github.com/ir-uam/contact-rec-axioms/wiki/EWC2)
* [EWC3](https://github.com/ir-uam/contact-rec-axioms/wiki/EWC3)
* [NDC](https://github.com/ir-uam/contact-rec-axioms/wiki/NDC)
* [CLNCS](https://github.com/ir-uam/contact-rec-axioms/wiki/CLNCS)
* [Degree](https://github.com/ir-uam/contact-rec-axioms/wiki/degree)
* [Validation](https://github.com/ir-uam/contact-rec-axioms/wiki/Validation)

## References
[1] Sanz-Cruzado, J., Castells, P.  Information Retrieval Models for Contact Recommendation in Social Networks. 
In: ECIR 2019: Advances in Information Retrieval, pp. 148–163. No. 11437 in LNCS, 
Springer International Publishing, Cologne, Germany (2019)
