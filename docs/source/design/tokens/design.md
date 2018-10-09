# Corda State Hierarchy Design

This paper describes a new approach to modelling tokens and agreements on Corda. There are two key intuitions behind the model: 

1. The notion of "fungibility" can be an attribute of any state type, not just assets and/or ownable things
2. The lifecycle of an asset or token should be orthogonal to the notion of ownership

## Additions to the core data model

The current versions of `ContractState`, `OwnableState` and `LinearState` in Corda Core are used as they exist today. 

A new `FungibleState` is added for the following reasons:

* `FungibleAsset` defines an amount property of type `Amount<Issued<T>>`, therefore there is an assumption that all fungible things are issued by a single well known party but this is not always the case. For example, permissionless cryptocurrencies like Bitcoin are generated periodically by a pool of pseudo-anonymous miners and Corda can support such cryptocurrencies.
* `FungibleAsset` implements `OwnableState`, as such there is an assumption that all fungible things are ownable. This is not always true as fungible derivative contracts exist, for example.

The new `FungibleState` is simply defined as:

```kotlin
interface FungibleState<T : TokenTypePointer> : ContractState {
    val amount: Amount<T>
}
```

Where `TokenTypePointer` is a class which allows us to easily resolve a `linearId` to the underlying `StateAndRef`. The significance of `TokenTypePointer` will be explained below.

```kotlin
class TokenTypePointer<T : TokenType>(val linearId: UniqueIdentifier) {
    fun resolve(services: ServiceHub): StateAndRef<T> {
    	TODO()
    }
}
```

`TokenType` at the highest-level is just a `LinearState`. However one can imagine interfaces which cater to different classes of token; asset backed token, native token, debt, equity, etc. `TokenType` is defined as:

```kotlin
interface TokenType : LinearState
```

The new `FungibleState` interface does not implement `OwnableState` or assume that all fungible things are `Issued`. Of course, fungible things can be issued and a new state type has been created to model ownable fungible things, otherwise known as tokens. 

`FungibleAsset` will remain part of Corda core to maintain backwards compatibility. However, it is recommended that it be deprecated in favour of the new `FungibleState` type.

## Tokens vs agreements

Excluding states that are created for workflow purposes, states can be split into two types: tokens and agreements. The key differentiator between tokens and agreements are that tokens are ownable and agreements involve multiple participants. The code for `Agreement` is included below: 

```kotlin
abstract class Agreement(
    final override val participants: List<AbstractParty>
) : ContractState {
    init {
        require(participants.size > 1) { 
            "Agreements must involve two or more participants." 
        }
        require(participants.toSet().size == participants.size) { 
            "All parties must be distinct in an agreement." 
        }
    }
}
```

We can see that `Agreement`s requires at least two `participants` and that the `participants` are distinct.

There is no equivelant `Token` abstract class because no assertions need to be made over any of the `Token`'s properties at object creation time.

## The four foundational state types

With the building blocks defined above, four base state types can be generated with the following combinations of interfaces and abstract classes:

![](/Users/rogerwillis/Desktop/state hierarchy.png)The table below is a clearer illustration of what the model enables:

|                  |           Token            |           Agreement            |
| :--------------: | :------------------------: | :----------------------------: |
|   **Fungible**   | `FungibleToken<TokenType>` | `FungibleAgreement<TokenType>` |
| **Non Fungible** |     `NonFungibleToken`     |     `NonFungibleAgreement`     |

Taking each state type in order:

**FungibleToken**

```kotlin
open class FungibleToken<T : TokenType>(
        override val amount: Amount<TokenTypePointer<T>>,
        override val owner: AbstractParty
) : FungibleState<T>, OwnableState {
    override val participants: List<AbstractParty> get() = listOf(owner)
    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        TODO()
    }
}
```

`FugibleToken` is something that can be split and merged and is ownable. It contains the following properties:

* An `amount` property of a  `TokenTypePointer` to a pre-defined `TokenType`
* An `owner` property of type `AbstractParty`, so the owner can be pseudo-anonymous, initially defined in `OwnableState`
* All states contain a `participants` property

It is easy to see that the `FungibleToken` is only concered with which party owns a specified amount of some `TokenType` which is defined elsewhere. As such `FungibleToken`s support three  simple behaviors: `issue`, `move`, `redeem`. The "token app" will contain flows to create `TokenType`s then issue, move and redeem tokens.

The `FungibleToken` class is defined as open so developers can create their own sub-classes, e.g. one which incorporates owner whitelists, for example.

`FungibleToken` coupled with `TokenType` is the Corda version of **ERC-20**.

**FungibleAgreement**

```kotlin
open class FungibleAgreement<T : TokenType>(
        override val amount: Amount<TokenTypePointer<T>>,
        participants: List<AbstractParty>
) : AgreementState(participants), FungibleState<T>
```

`FungibleAgreement` can now exist because `FungibleState` does not implement `OwnableState`. It is used in the same way as `FungibleToken` – a `TokenType` is required. It’s really just syntactic sugar as `FungibleToken` could be used instead.

**NonFungibleToken**

```kotlin
interface NonFungibleToken : TokenType, NonFungibleState, OwnableState
```

`NonFungibleToken` is simply a `TokenType`! This is because there is no need for there to be an amount of it as only one exists. Only use `NonFungibleToken` if we know definitively there will only ever be one token representing this “thing”. It is worth nothing that it is possible only one physical “thing” exists but many tokens are issued in respect of it to represent fractional ownership. 

Like `FungibleAgreement`, `NonFungibleToken` is mainly syntatic sugar. It is possible that all tokens could be represented with `FungibleToken`. The way to do this would be to create a `TokenType` then issue a `FungibleToken` with `amount` set to the smallest unit. Issuing multiple units implies fractional ownership in a single entity.

To create a non fungible token, the `NonFungibleToken` interface should be implemented.

This is **ERC-721**.

**NonFungibleAgreement** 

```kotlin
abstract class NonFungibleAgreement(
		participants: List<AbstractParty>
) : NonFungibleState, AgreementState(participants)
```

`NonFungibleAgreement` implements `NonFungibleState` and `AgreementState`. This is just an agreement which evolves over time. To create a non fungible agreement, developers should sub-class `NonFungibleAgreement` and add the required properties.

### Relationship between `TokenType` and `FungibleToken`

`TokenType` is the key to making all of this work. The key idea here is the intuition that the issuers of tokens should manage the lifecycle of the tokens independently to the process which manages ownership of the tokens.

`TokenType`s are defined as `LinearState`s, this is because we expect `TokenType`s to have their own lifecycle. 

Take a stock issued by *MEGA CORP*, for instance… It can be issued by MEGA CORP, the company may then announce a divided, pay a dividend, perform a share split, etc. This should all be managed inside the `TokenType` state. 

MEGA CORP is at liberty to define which ever properties and methods they deem necessary on the `TokenState`. Only MEGA CORP has the capability of updating the `TokenState`. Other parties on the Corda Network that hold MEGA CORP stock, use the `TokenState` as a `ReferenceState` in transactions. It is likely that MEGA CORP would distribute the `TokenType` updates via data distribution groups. Holders of MEGA CORP stock would subscribe to updates to ensure they have the most up-to-date version of the `TokenType` which reflects all recent lifecycle events.

The `TokenType` is linked to the `FungibleToken` state via the `TokenTypePointer`. The pointer includes the `linearId` of the `TokenType` and a implements a `resolve()` method. We cannot link the `TokenType` by `StateRef` as the `FungibleToken` would require updating each time the `TokenType` is updated! `Resolve()` allows developers to resolve the `linearId` to the `TokenType` state inside a flow. Conceptually, this is similar to the process where a `StateRef` is resolved to a `StateAndRef` .

![](/Users/rogerwillis/Desktop/Screen Shot 2018-10-08 at 17.27.11.png)

**Creating and issuing tokens**

The process of creating and issuing digital assets is now as follows:

1. Create a a new `TokenType`. A `TokenType` is any state which implements the `TokenType` class. For now it is just a marker interface which implements `LinearState`.
2. Issue a new `FungibleToken` for some amount of the newly created `TokenType`. The transaction which issues the `FungibleToken`s references the `TokenType` as follows:

![](/Users/rogerwillis/Desktop/Screen Shot 2018-10-08 at 16.48.13.png)

The flow for issuing new `FungibleToken`s of a particular `TokenType` now looks something like: 

```kotlin
class IssueToken<T : TokenType>(
        val amount: Amount<T>, 
        val tokenTypeStateAndRef: StateAndRef<T>,
        val to: Party
) : FlowLogic<SignedTransaction>() {
    override fun call(): SignedTransaction {
        val tokenTypeState = tokenTypeStateAndRef.state.data
        // Convert the TokenType to a TokenTypePointer.
        val tokenTypePointer = tokenTypeState.toTokenTypePointer()
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val utx = TransactionBuilder(notary).apply {
            val fungibleToken = FungibleToken(Amount(10, tokenTypePointer), to)
            addOutputState(fungibleToken, FungibleTokenContract.contractId)
            // Convert the StateAndRef to a ReferencedStateAndRef.
            addReferenceState(tokenTypeStateAndRef.referenced())
        }
        val stx = serviceHub.signInitialTransaction(utx)
        return subFlow(FinalityFlow(stx))
    }
}
```

Some additional things need doing to ensure that the recipient of the newly issued `FungibleToken`s persists the `TokenType` reference state in their vault. The `SendTransactionFlow`/`ReceiveTransacrtionFlow`s can be used.

**Querying the vault for FungibleTokens**

When querying the vault for fungible tokens or performing coin selection, flow developers must pass the vault query service the `TokenType` they wish to query for. The vault service will then querying using the `linearId` of the `TokenType` provided.

**Making it easy to use TokenTypes**

It is likely that a TokenType lookup service is required. When a token name is provided, e.g. "GBP", the service should return return all `TokenType` matches. To make things even easier, each `TokenType` should define a unique human readable name. 