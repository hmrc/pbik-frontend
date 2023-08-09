# pbik-frontend

This service provides the Play Framework http endpoint for employers requests. The purpose of this service is to allow employers to tell HMRC what benefits or expenses they are payrolling so that they no longer have to submit a P11D form for every employee. It retrieves a list of benefits or expenses from the PBIK microservice and allows the employer to exclude certain individuals, these updates are then sent to HMRC backend systems.

Once authenticated the employer has access to the pbik-frontend functionality which allows

### Getting started
#### Locally:
- To run all PBIK and dependant services in Service Manager2, run:
```
sm2 --start PBIK_ALL
```

- To run specific set of tests/ scenarios, following commands are useful
```To run the whole spec
sbt 'testOnly *TierConnectorSpec'
```

```To run a single test/ scenario
sbt 'testOnly *TierConnectorSpec -- -z partialTextOnTheTestName'
```

### Overview 

#### Summary Section

* An overview of which benefits the employer has registered to payroll for current year ( CY )
* An overview of which benefits the employer has registered to payroll for next year ( CY+1 )
* Ability to add, remove, exclude individuals by selecting the relevant link

-----
##### Current Year Plus One (CY+1)

* An overview of which benefits the employer has registered to payroll for next year (CY+1)
* The ability to ADD benefits to Current Year Plus One
* The ability to REMOVE benefits to Current Year Plus One
* The ability to ADD individuals to an Exclusion List for registered benefits
* The ability to REMOVE individuals from the Exclusion List for registered benefits

##### Exclusions

* The ability to exclude an individual for Current Year
* The ability to exclude an individual for Next Year
* The ability to rescind an exclusion for Next Current Year

### Configuration

survey.url - Feedback page location when the user signs out of the service <br />
pbik.banner.start - Overview page banner start date to be shown when annual tax coding is about to start (21 December) <br />
pbik.banner.end - Overview page banner end date to be shown when annual tax coding is about to end <br />
enabled.cy - Enable CY registrations (Caution when enabling, needs to be discussed with DSM/ stakeholders) <br />
enabled.eil - Exclusion functionality enabled <br />
unsupported.biks.cy - Which benefits can't be registered in CY (Caution, may need NPS config change prior to changing here) <br />
unsupported.biks.cy1 - Which benefits can't be registered in CY+1 (Caution, may need NPS config change prior to changing here) <br />
decommissioned.biks - Which benefits no longer exist <br />

### Navigation

        Summary -> 
          -> Choose benefits (CY+1) -> Confirm  -> Benefit(s) added
          -> Choose benefits (CY)   -> Confirm  -> Benefit(s) added
        
          -> Exclude (CY+1)     -> Overview -> Choose Nino/ No-nino -> Search -> Confirm -> Individual excluded
                                              -> Confirm removal -> Individual removed
                                              
          -> Exclude (CY)       -> Overview -> Choose Nino/ No-nino -> Search -> Confirm -> Individual excluded
                                              -> Confirm removal -> Individual removed
                                              

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
